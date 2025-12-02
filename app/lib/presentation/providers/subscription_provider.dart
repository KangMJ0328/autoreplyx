import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/datasources/remote/subscription_remote_datasource.dart';
import '../../data/models/subscription_model.dart';

final subscriptionPlansProvider = FutureProvider.autoDispose<List<SubscriptionPlan>>((ref) async {
  final dataSource = ref.watch(subscriptionRemoteDataSourceProvider);
  return dataSource.getPlans();
});

final currentSubscriptionProvider = AsyncNotifierProvider<SubscriptionNotifier, UserSubscription?>(() {
  return SubscriptionNotifier();
});

class SubscriptionNotifier extends AsyncNotifier<UserSubscription?> {
  @override
  Future<UserSubscription?> build() async {
    try {
      final dataSource = ref.watch(subscriptionRemoteDataSourceProvider);
      return dataSource.getCurrentSubscription();
    } catch (e) {
      return null;
    }
  }

  Future<void> subscribe(String planId, String paymentMethod) async {
    final dataSource = ref.read(subscriptionRemoteDataSourceProvider);
    final subscription = await dataSource.subscribe(
      PaymentRequest(planId: planId, paymentMethod: paymentMethod),
    );
    state = AsyncValue.data(subscription);
  }

  Future<void> cancel() async {
    final dataSource = ref.read(subscriptionRemoteDataSourceProvider);
    await dataSource.cancelSubscription();
    ref.invalidateSelf();
  }

  Future<void> toggleAutoRenew(bool autoRenew) async {
    final dataSource = ref.read(subscriptionRemoteDataSourceProvider);
    final subscription = await dataSource.toggleAutoRenew(autoRenew);
    state = AsyncValue.data(subscription);
  }
}

// Selected plan for purchase
final selectedPlanProvider = StateProvider<SubscriptionPlan?>((ref) => null);
