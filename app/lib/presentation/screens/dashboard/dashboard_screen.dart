import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/theme/app_colors.dart';
import '../../providers/auth_provider.dart';
import '../../providers/dashboard_provider.dart';
import '../../widgets/common/loading_overlay.dart';
import '../../widgets/common/error_view.dart';

class DashboardScreen extends ConsumerWidget {
  const DashboardScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final user = ref.watch(currentUserProvider);
    final statsAsync = ref.watch(dashboardStatsProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text(user?.brandName ?? 'AutoReplyX'),
        actions: [
          IconButton(
            icon: const Icon(Icons.notifications_outlined),
            onPressed: () {},
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: () async {
          ref.invalidate(dashboardStatsProvider);
          await ref.read(authStateProvider.notifier).refreshProfile();
        },
        child: SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Usage card
              _buildUsageCard(context, user),
              const SizedBox(height: 16),
              // Stats
              statsAsync.when(
                data: (stats) => _buildStatsGrid(stats),
                loading: () => const LoadingWidget(),
                error: (e, _) => ErrorView(
                  message: e.toString(),
                  onRetry: () => ref.invalidate(dashboardStatsProvider),
                ),
              ),
              const SizedBox(height: 24),
              // Quick actions
              _buildQuickActions(context),
              const SizedBox(height: 24),
              // Recent activity header
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text(
                    '최근 활동',
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  TextButton(
                    onPressed: () => context.go('/logs'),
                    child: const Text('전체보기'),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              _buildRecentActivity(),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildUsageCard(BuildContext context, user) {
    final used = user?.messageCount ?? 0;
    final limit = user?.messageLimit ?? 100;
    final percentage = limit > 0 ? (used / limit) : 0.0;

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [AppColors.primary, AppColors.primaryDark],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '${user?.plan ?? 'Free'} 플랜',
                    style: const TextStyle(
                      color: Colors.white70,
                      fontSize: 14,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    '$used / $limit',
                    style: const TextStyle(
                      color: AppColors.white,
                      fontSize: 28,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ],
              ),
              OutlinedButton(
                onPressed: () => context.go('/subscription'),
                style: OutlinedButton.styleFrom(
                  foregroundColor: AppColors.white,
                  side: const BorderSide(color: AppColors.white),
                ),
                child: const Text('업그레이드'),
              ),
            ],
          ),
          const SizedBox(height: 16),
          ClipRRect(
            borderRadius: BorderRadius.circular(8),
            child: LinearProgressIndicator(
              value: percentage,
              backgroundColor: Colors.white24,
              valueColor: AlwaysStoppedAnimation<Color>(
                percentage > 0.8 ? AppColors.warning : AppColors.white,
              ),
              minHeight: 8,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            '이번 달 메시지 사용량',
            style: TextStyle(
              color: Colors.white.withOpacity(0.7),
              fontSize: 12,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStatsGrid(stats) {
    return GridView.count(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      crossAxisCount: 2,
      mainAxisSpacing: 12,
      crossAxisSpacing: 12,
      childAspectRatio: 1.5,
      children: [
        _buildStatCard(
          '오늘 메시지',
          '${stats.todayMessages}',
          Icons.message_outlined,
          AppColors.info,
        ),
        _buildStatCard(
          'AI 응답',
          '${stats.aiResponses}',
          Icons.smart_toy_outlined,
          AppColors.secondary,
        ),
        _buildStatCard(
          '활성 채널',
          '${stats.activeChannels}',
          Icons.connect_without_contact_outlined,
          AppColors.accent,
        ),
        _buildStatCard(
          '활성 규칙',
          '${stats.activeRules}',
          Icons.rule_outlined,
          AppColors.primary,
        ),
      ],
    );
  }

  Widget _buildStatCard(
      String label, String value, IconData icon, Color color) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppColors.grey200),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(icon, color: color, size: 24),
          const SizedBox(height: 8),
          Text(
            value,
            style: const TextStyle(
              fontSize: 24,
              fontWeight: FontWeight.bold,
            ),
          ),
          Text(
            label,
            style: const TextStyle(
              fontSize: 12,
              color: AppColors.grey500,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildQuickActions(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: _buildActionButton(
            context,
            '규칙 추가',
            Icons.add_circle_outline,
            () => context.go('/rules/new'),
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: _buildActionButton(
            context,
            '채널 연결',
            Icons.link,
            () => context.go('/channels'),
          ),
        ),
      ],
    );
  }

  Widget _buildActionButton(
      BuildContext context, String label, IconData icon, VoidCallback onTap) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(12),
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 16),
        decoration: BoxDecoration(
          color: AppColors.grey50,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: AppColors.grey200),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, color: AppColors.primary),
            const SizedBox(width: 8),
            Text(
              label,
              style: const TextStyle(
                fontWeight: FontWeight.w500,
                color: AppColors.grey700,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildRecentActivity() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppColors.grey200),
      ),
      child: const Center(
        child: Padding(
          padding: EdgeInsets.all(24),
          child: Column(
            children: [
              Icon(
                Icons.inbox_outlined,
                size: 48,
                color: AppColors.grey400,
              ),
              SizedBox(height: 12),
              Text(
                '아직 활동 내역이 없습니다',
                style: TextStyle(
                  color: AppColors.grey500,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
