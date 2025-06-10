package com.example.progetto_sistemidistribuiti.service;

import com.example.progetto_sistemidistribuiti.dto.UserProfileDto;
import com.example.progetto_sistemidistribuiti.model.UserProfile;
import com.example.progetto_sistemidistribuiti.repository.UserProfileRepository;
import com.example.progetto_sistemidistribuiti.support.InvalidCredentialsException;
import com.example.progetto_sistemidistribuiti.support.UserNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserProfileRepository userRepository;

    @Value("${aws.access.key}")
    private String accessKey;

    @Value("${cognito.client.id}")
    private String clientId;

    @Value("${aws.secret.key}")
    private String secretKey;

    @Value("${aws.region}")
    private String awsRegion;

    @Value("${cognito.id}")
    private String userPoolId;

    private static final String PASSWORD_PATTERN =
            "^(?=.*[A-Z])(?=.*\\d)(?=.*[_\\-.:,;])[A-Za-z\\d_\\-.:,;]{8,}$";

    private static final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);

    public void createUser(UserProfileDto userProfileDto) {

        if (!isValidPassword(userProfileDto.getTemporaryPassword())) {
            throw new IllegalArgumentException(
                    "La password deve contenere: almeno 1 lettera maiuscola, " +
                            "almeno 1 numero, almeno 1 carattere speciale (_-.:,;) e " +
                            "deve essere di almeno 8 caratteri"
            );
        }

        try (CognitoIdentityProviderClient cognitoClient = CognitoIdentityProviderClient.builder()
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)
                        )
                )
                .region(Region.of(awsRegion))
                .build()) {


            AdminCreateUserRequest request = AdminCreateUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(userProfileDto.getUsername())
                    .temporaryPassword(userProfileDto.getTemporaryPassword())
                    .userAttributes(
                            AttributeType.builder().name("email").value(userProfileDto.getEmail()).build(),
                            AttributeType.builder().name("email_verified").value("true").build()
                    )
                    .messageAction(MessageActionType.SUPPRESS) // non invia mail
                    .build();

            AdminCreateUserResponse response = cognitoClient.adminCreateUser(request);


            AdminSetUserPasswordRequest setPasswordRequest = AdminSetUserPasswordRequest.builder()
                    .userPoolId(userPoolId)
                    .username(userProfileDto.getUsername())
                    .password(userProfileDto.getTemporaryPassword())
                    .permanent(true)
                    .build();
            cognitoClient.adminSetUserPassword(setPasswordRequest);


            AdminGetUserResponse userResponse = cognitoClient.adminGetUser(AdminGetUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(userProfileDto.getUsername())
                    .build());

            String status = userResponse.userStatusAsString(); // UNCONFIRMED, CONFIRMED, FORCE_CHANGE_PASSWORD
            if (!"CONFIRMED".equals(status)) {
                AdminConfirmSignUpRequest confirmRequest = AdminConfirmSignUpRequest.builder()
                        .userPoolId(userPoolId)
                        .username(userProfileDto.getUsername())
                        .build();
                cognitoClient.adminConfirmSignUp(confirmRequest);
            } else {
                System.out.println("ℹ️ Utente già confermato.");
            }


            UserProfile userProfile = new UserProfile();
            userProfile.setEmail(userProfileDto.getEmail());
            userProfile.setFullName(userProfileDto.getFullName());
            userProfile.setDocuments(new ArrayList<>());

            userRepository.save(userProfile);

            System.out.println("Utente creato in Cognito: " + response.user().username());

        } catch (UsernameExistsException e) {
            throw new RuntimeException("Utente già esistente", e);
        } catch (Exception e) {
            throw new RuntimeException("Errore durante la creazione utente Cognito", e);
        }
    }

    public String loginUser(String username, String password) {
        try (CognitoIdentityProviderClient cognitoClient = CognitoIdentityProviderClient.builder()
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)
                        )
                )
                .region(Region.of(awsRegion))
                .build()) {

            Map<String, String> authParams = new HashMap<>();
            authParams.put("USERNAME", username);
            authParams.put("PASSWORD", password);

            InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                    .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                    .clientId(clientId)
                    .authParameters(authParams)
                    .build();

            InitiateAuthResponse response = cognitoClient.initiateAuth(authRequest);

             return response.authenticationResult().idToken();

        } catch (NotAuthorizedException e) {
            throw new InvalidCredentialsException("Email o password non corretti", e);
        } catch (UserNotConfirmedException e) {
            throw new RuntimeException("Account non confermato. Contatta l'amministratore.", e);
        } catch (Exception e) {
            throw new RuntimeException("Errore durante il login", e);
        }
    }

    private boolean isValidPassword(String password) {
        return password != null && pattern.matcher(password).matches();
    }

    @Transactional
    public List<UserProfile> getAllUsers() {
        return userRepository.findAll();
    }

    public UserProfile getUserById(String id) throws UserNotFoundException {
        return userRepository.findById(id)
                .orElseThrow(UserNotFoundException::new);
    }

    public UserProfile getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utente con email " + email + " non trovato"));
    }

    @Transactional
    public void deleteUser(String id) {
        userRepository.deleteById(id);
    }
}
