import 'user_model.dart';

class LoginRequest {
  final String email;
  final String password;

  LoginRequest({required this.email, required this.password});

  Map<String, dynamic> toJson() => {'email': email, 'password': password};
}

class RegisterRequest {
  final String email;
  final String password;
  final String brandName;
  final String? industry;

  RegisterRequest({
    required this.email,
    required this.password,
    required this.brandName,
    this.industry,
  });

  Map<String, dynamic> toJson() => {
    'email': email,
    'password': password,
    'brandName': brandName,
    'industry': industry,
  };
}

class AuthResponse {
  final String accessToken;
  final String? refreshToken;
  final UserModel user;

  AuthResponse({
    required this.accessToken,
    this.refreshToken,
    required this.user,
  });

  factory AuthResponse.fromJson(Map<String, dynamic> json) {
    return AuthResponse(
      accessToken: json['accessToken'] as String? ?? json['token'] as String,
      refreshToken: json['refreshToken'] as String?,
      user: UserModel.fromJson(json['user'] as Map<String, dynamic>),
    );
  }
}
