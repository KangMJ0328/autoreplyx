import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/constants/api_constants.dart';
import '../../models/subscription_model.dart';

final subscriptionRemoteDataSourceProvider = Provider<SubscriptionRemoteDataSource>((ref) {
  return SubscriptionRemoteDataSource(ref.watch(dioClientProvider));
});

class SubscriptionRemoteDataSource {
  final DioClient _dioClient;

  SubscriptionRemoteDataSource(this._dioClient);

  Future<List<SubscriptionPlan>> getPlans() async {
    final response = await _dioClient.get(ApiConstants.subscriptionPlans);
    return (response.data as List)
        .map((json) => SubscriptionPlan.fromJson(json))
        .toList();
  }

  Future<UserSubscription> getCurrentSubscription() async {
    final response = await _dioClient.get(ApiConstants.subscription);
    return UserSubscription.fromJson(response.data);
  }

  Future<UserSubscription> subscribe(PaymentRequest request) async {
    final response = await _dioClient.post(
      ApiConstants.subscriptionPayment,
      data: request.toJson(),
    );
    return UserSubscription.fromJson(response.data);
  }

  Future<void> cancelSubscription() async {
    await _dioClient.delete(ApiConstants.subscription);
  }

  Future<UserSubscription> toggleAutoRenew(bool autoRenew) async {
    final response = await _dioClient.patch(
      '${ApiConstants.subscription}/auto-renew',
      data: {'autoRenew': autoRenew},
    );
    return UserSubscription.fromJson(response.data);
  }
}
