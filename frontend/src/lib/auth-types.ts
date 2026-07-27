export interface AuthUser {
  userId: number;
  email: string;
  fullName: string;
  role: string;
  emailVerified: boolean;
}

export interface AuthSession extends AuthUser {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface RegisterPayload {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
}
