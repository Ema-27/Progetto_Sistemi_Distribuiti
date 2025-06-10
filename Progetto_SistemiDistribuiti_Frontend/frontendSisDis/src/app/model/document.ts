import { UserRegistration }  from './user-registration'

export interface Document {
  owner: UserRegistration;
  id: number;
  title: string;
  year: number;
  authors: string;
  paper: string;
  fileUrl: string;
  keywords: Array<string>;
  useRake: boolean;
  showAllKeywords?: boolean; 
}