import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/constants/api_constants.dart';
import '../../models/auth_model.dart';
import '../../models/user_model.dart';

final authRemoteDataSourceProvider = Provider<AuthRemoteDataSource>((ref) {
  return AuthRemoteDataSource(ref.watch(dioClientProvider));
});

class AuthRemoteDataSource {
  final DioClient _dioClient;

  AuthRemoteDataSource(this._dioClient);

  Future<AuthResponse> login(LoginRequest request) async {
    final response = await _dioClient.post(
      ApiConstants.login,
      data: request.toJson(),
    );
    return AuthResponse.fromJson(response.data);
  }

  Future<AuthResponse> register(RegisterRequest request) async {
    final response = await _dioClient.post(
      ApiConstants.register,
      data: request.toJson(),
    );
    return AuthResponse.fromJson(response.data);
  }

  Future<UserModel> getProfile() async {
    final response = await _dioClient.get(ApiConstants.profile);
    return UserModel.fromJson(response.data);
  }

  Future<UserModel> updateProfile(Map<String, dynamic> data) async {
    final response = await _dioClient.put(
      ApiConstants.updateProfile,
      data: data,
    );
    return UserModel.fromJson(response.data);
  }

  Future<void> logout() async {
    await _dioClient.post(ApiConstants.logout);
  }

  Future<AuthResponse> refreshToken(String refreshToken) async {
    final response = await _dioClient.post(
      ApiConstants.refreshToken,
      data: {'refreshToken': refreshToken},
    );
    return AuthResponse.fromJson(response.data);
  }
}
