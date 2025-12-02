import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_colors.dart';
import '../../../data/models/subscription_model.dart';
import '../../providers/subscription_provider.dart';
import '../../providers/auth_provider.dart';
import '../../widgets/common/loading_overlay.dart';
import '../../widgets/common/error_view.dart';

class SubscriptionScreen extends ConsumerStatefulWidget {
  const SubscriptionScreen({super.key});

  @override
  ConsumerState<SubscriptionScreen> createState() => _SubscriptionScreenState();
}

class _SubscriptionScreenState extends ConsumerState<SubscriptionScreen> {
  bool _isLoading = false;

  @override
  Widget build(BuildContext context) {
    final user = ref.watch(currentUserProvider);
    final plansAsync = ref.watch(subscriptionPlansProvider);
    final currentPlan = user?.plan ?? 'free';

    return Scaffold(
      appBar: AppBar(
        title: const Text('구독 관리'),
      ),
      body: LoadingOverlay(
        isLoading: _isLoading,
        child: plansAsync.when(
          data: (plans) => SingleChildScrollView(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Current usage
                _buildUsageCard(user),
                const SizedBox(height: 24),

                const Text(
                  '플랜 선택',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 12),

                // Plans
                ...plans.map((plan) => _buildPlanCard(plan, currentPlan)),
              ],
            ),
          ),
          loading: () => const LoadingWidget(),
          error: (e, _) => ErrorView(
            message: e.toString(),
            onRetry: () => ref.invalidate(subscriptionPlansProvider),
          ),
        ),
      ),
    );
  }

  Widget _buildUsageCard(user) {
    final used = user?.messageCount ?? 0;
    final limit = user?.messageLimit ?? 100;
    final percentage = limit > 0 ? (used / limit) : 0.0;

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.grey200),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                '이번 달 사용량',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: AppColors.primary.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(4),
                ),
                child: Text(
                  user?.plan ?? 'Free',
                  style: const TextStyle(
                    fontSize: 12,
                    color: AppColors.primary,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          Row(
            children: [
              Text(
                '$used',
                style: const TextStyle(
                  fontSize: 32,
                  fontWeight: FontWeight.bold,
                  color: AppColors.primary,
                ),
              ),
              Text(
                ' / $limit',
                style: const TextStyle(
                  fontSize: 24,
                  color: AppColors.grey400,
                ),
              ),
              const Text(
                ' 건',
                style: TextStyle(
                  fontSize: 14,
                  color: AppColors.grey500,
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          ClipRRect(
            borderRadius: BorderRadius.circular(8),
            child: LinearProgressIndicator(
              value: percentage,
              backgroundColor: AppColors.grey200,
              valueColor: AlwaysStoppedAnimation<Color>(
                percentage > 0.8 ? AppColors.warning : AppColors.primary,
              ),
              minHeight: 8,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            '${(percentage * 100).toStringAsFixed(0)}% 사용',
            style: const TextStyle(
              fontSize: 12,
              color: AppColors.grey500,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildPlanCard(SubscriptionPlan plan, String currentPlan) {
    final isCurrentPlan = plan.id == currentPlan;
    final isPopular = plan.isPopular;

    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: isPopular ? AppColors.primary : AppColors.grey200,
          width: isPopular ? 2 : 1,
        ),
      ),
      child: Stack(
        children: [
          if (isPopular)
            Positioned(
              top: 0,
              right: 16,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                decoration: const BoxDecoration(
                  color: AppColors.primary,
                  borderRadius: BorderRadius.only(
                    bottomLeft: Radius.circular(8),
                    bottomRight: Radius.circular(8),
                  ),
                ),
                child: const Text(
                  '인기',
                  style: TextStyle(
                    color: AppColors.white,
                    fontSize: 12,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ),
            ),
          Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Text(
                      plan.name,
                      style: const TextStyle(
                        fontSize: 20,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    if (isCurrentPlan) ...[
                      const SizedBox(width: 8),
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 8,
                          vertical: 2,
                        ),
                        decoration: BoxDecoration(
                          color: AppColors.secondary.withOpacity(0.1),
                          borderRadius: BorderRadius.circular(4),
                        ),
                        child: const Text(
                          '현재 플랜',
                          style: TextStyle(
                            fontSize: 11,
                            color: AppColors.secondary,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ),
                    ],
                  ],
                ),
                const SizedBox(height: 4),
                Text(
                  plan.description,
                  style: const TextStyle(
                    color: AppColors.grey500,
                    fontSize: 13,
                  ),
                ),
                const SizedBox(height: 12),
                Row(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    Text(
                      plan.formattedPrice,
                      style: const TextStyle(
                        fontSize: 28,
                        fontWeight: FontWeight.bold,
                        color: AppColors.grey900,
                      ),
                    ),
                    if (plan.price > 0)
                      const Padding(
                        padding: EdgeInsets.only(bottom: 4, left: 4),
                        child: Text(
                          '/월',
                          style: TextStyle(
                            fontSize: 14,
                            color: AppColors.grey500,
                          ),
                        ),
                      ),
                  ],
                ),
                const SizedBox(height: 16),
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: AppColors.grey50,
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Row(
                    children: [
                      const Icon(
                        Icons.mail_outline,
                        size: 18,
                        color: AppColors.grey600,
                      ),
                      const SizedBox(width: 8),
                      Text(
                        '월 ${plan.formattedMessageLimit} 메시지',
                        style: const TextStyle(
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 12),
                ...plan.features.map((feature) => Padding(
                      padding: const EdgeInsets.only(bottom: 8),
                      child: Row(
                        children: [
                          const Icon(
                            Icons.check_circle,
                            size: 16,
                            color: AppColors.secondary,
                          ),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              feature,
                              style: const TextStyle(
                                fontSize: 13,
                                color: AppColors.grey700,
                              ),
                            ),
                          ),
                        ],
                      ),
                    )),
                const SizedBox(height: 16),
                SizedBox(
                  width: double.infinity,
                  child: isCurrentPlan
                      ? OutlinedButton(
                          onPressed: null,
                          child: const Text('현재 사용 중'),
                        )
                      : ElevatedButton(
                          onPressed: () => _selectPlan(plan),
                          style: isPopular
                              ? null
                              : ElevatedButton.styleFrom(
                                  backgroundColor: AppColors.grey700,
                                ),
                          child: Text(
                            plan.price == 0 ? '무료로 시작' : '업그레이드',
                          ),
                        ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _selectPlan(SubscriptionPlan plan) async {
    if (plan.price == 0) {
      // Free plan - just subscribe
      setState(() => _isLoading = true);
      try {
        await ref.read(currentSubscriptionProvider.notifier).subscribe(
              plan.id,
              'free',
            );
        await ref.read(authStateProvider.notifier).refreshProfile();
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('플랜이 변경되었습니다')),
          );
        }
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(e.toString())),
          );
        }
      } finally {
        if (mounted) setState(() => _isLoading = false);
      }
      return;
    }

    // Show payment sheet for paid plans
    _showPaymentSheet(plan);
  }

  void _showPaymentSheet(SubscriptionPlan plan) {
    showModalBottomSheet(
      context: context,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) => Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '${plan.name} 구독',
              style: const TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              '${plan.formattedPrice}/월',
              style: const TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.bold,
                color: AppColors.primary,
              ),
            ),
            const SizedBox(height: 20),
            const Text(
              '결제 수단',
              style: TextStyle(fontWeight: FontWeight.w500),
            ),
            const SizedBox(height: 12),
            _buildPaymentMethod('card', '신용/체크카드', Icons.credit_card),
            const SizedBox(height: 8),
            _buildPaymentMethod('kakao', '카카오페이', Icons.account_balance_wallet),
            const SizedBox(height: 20),
            const Text(
              '* 구독은 매월 자동 갱신되며, 언제든지 취소할 수 있습니다.',
              style: TextStyle(
                fontSize: 12,
                color: AppColors.grey500,
              ),
            ),
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: () async {
                  Navigator.pop(context);
                  setState(() => _isLoading = true);
                  try {
                    await ref
                        .read(currentSubscriptionProvider.notifier)
                        .subscribe(plan.id, 'card');
                    await ref.read(authStateProvider.notifier).refreshProfile();
                    if (mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(content: Text('구독이 완료되었습니다')),
                      );
                    }
                  } catch (e) {
                    if (mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(content: Text(e.toString())),
                      );
                    }
                  } finally {
                    if (mounted) setState(() => _isLoading = false);
                  }
                },
                child: const Text('결제하기'),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPaymentMethod(String id, String name, IconData icon) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        border: Border.all(color: AppColors.grey200),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        children: [
          Icon(icon, color: AppColors.grey600),
          const SizedBox(width: 12),
          Text(name),
          const Spacer(),
          Radio(
            value: id,
            groupValue: 'card',
            onChanged: (_) {},
          ),
        ],
      ),
    );
  }
}
