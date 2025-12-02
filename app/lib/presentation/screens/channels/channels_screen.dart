import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:url_launcher/url_launcher.dart';
import '../../../core/theme/app_colors.dart';
import '../../providers/channels_provider.dart';
import '../../widgets/common/loading_overlay.dart';
import '../../widgets/common/error_view.dart';

class ChannelsScreen extends ConsumerStatefulWidget {
  const ChannelsScreen({super.key});

  @override
  ConsumerState<ChannelsScreen> createState() => _ChannelsScreenState();
}

class _ChannelsScreenState extends ConsumerState<ChannelsScreen> {
  bool _isConnecting = false;

  Future<void> _connectInstagram() async {
    setState(() => _isConnecting = true);

    try {
      final authUrl = await ref.read(channelsProvider.notifier).getInstagramAuthUrl();
      final uri = Uri.parse(authUrl);
      if (await canLaunchUrl(uri)) {
        await launchUrl(uri, mode: LaunchMode.externalApplication);
      } else {
        throw Exception('인스타그램 연결 페이지를 열 수 없습니다');
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(e.toString()),
            backgroundColor: AppColors.error,
          ),
        );
      }
    } finally {
      if (mounted) {
        setState(() => _isConnecting = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final channelsAsync = ref.watch(channelsProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('채널 관리'),
      ),
      body: LoadingOverlay(
        isLoading: _isConnecting,
        message: '연결 중...',
        child: channelsAsync.when(
          data: (channels) => RefreshIndicator(
            onRefresh: () => ref.read(channelsProvider.notifier).refresh(),
            child: ListView(
              padding: const EdgeInsets.all(16),
              children: [
                // Instagram card
                _buildInstagramCard(channels),
                const SizedBox(height: 24),
                // Coming soon
                _buildComingSoonSection(),
              ],
            ),
          ),
          loading: () => const LoadingWidget(),
          error: (e, _) => ErrorView(
            message: e.toString(),
            onRetry: () => ref.read(channelsProvider.notifier).refresh(),
          ),
        ),
      ),
    );
  }

  Widget _buildInstagramCard(List channels) {
    final instagram = channels.where((c) => c.platform == 'instagram').firstOrNull;
    final isConnected = instagram?.isConnected ?? false;

    return Container(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: isConnected
              ? [AppColors.primary, AppColors.primaryDark]
              : AppColors.instagramGradient,
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: Colors.white.withOpacity(0.2),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: const Icon(
                    Icons.camera_alt,
                    color: AppColors.white,
                    size: 28,
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        'Instagram',
                        style: TextStyle(
                          color: AppColors.white,
                          fontSize: 20,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      Text(
                        isConnected
                            ? '@${instagram?.username ?? ''}'
                            : '연결되지 않음',
                        style: TextStyle(
                          color: Colors.white.withOpacity(0.8),
                          fontSize: 14,
                        ),
                      ),
                    ],
                  ),
                ),
                if (isConnected)
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 12,
                      vertical: 6,
                    ),
                    decoration: BoxDecoration(
                      color: AppColors.secondary,
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: const Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(
                          Icons.check_circle,
                          color: AppColors.white,
                          size: 16,
                        ),
                        SizedBox(width: 4),
                        Text(
                          '연결됨',
                          style: TextStyle(
                            color: AppColors.white,
                            fontSize: 12,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ],
                    ),
                  ),
              ],
            ),
            const SizedBox(height: 20),
            if (isConnected) ...[
              // Connected state
              Row(
                children: [
                  Expanded(
                    child: _buildStatusItem(
                      '상태',
                      instagram?.isActive == true ? '활성' : '비활성',
                      instagram?.isActive == true
                          ? Icons.check_circle
                          : Icons.pause_circle,
                    ),
                  ),
                  Expanded(
                    child: _buildStatusItem(
                      '자동 응답',
                      instagram?.isActive == true ? 'ON' : 'OFF',
                      Icons.smart_toy,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton(
                      onPressed: () {
                        ref.read(channelsProvider.notifier).toggleChannel(
                              instagram!.id,
                              !instagram.isActive,
                            );
                      },
                      style: OutlinedButton.styleFrom(
                        foregroundColor: AppColors.white,
                        side: const BorderSide(color: AppColors.white),
                      ),
                      child: Text(instagram?.isActive == true
                          ? '일시 중지'
                          : '다시 시작'),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: OutlinedButton(
                      onPressed: () => _showDisconnectDialog(instagram!.id),
                      style: OutlinedButton.styleFrom(
                        foregroundColor: Colors.white70,
                        side: const BorderSide(color: Colors.white54),
                      ),
                      child: const Text('연결 해제'),
                    ),
                  ),
                ],
              ),
            ] else ...[
              // Not connected state
              const Text(
                'Instagram 비즈니스 계정을 연결하여\nDM, 댓글, 멘션에 자동으로 응답하세요.',
                style: TextStyle(
                  color: Colors.white70,
                  fontSize: 14,
                ),
              ),
              const SizedBox(height: 16),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: _connectInstagram,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.white,
                    foregroundColor: AppColors.primary,
                  ),
                  child: const Text('Instagram 연결하기'),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildStatusItem(String label, String value, IconData icon) {
    return Row(
      children: [
        Icon(icon, color: Colors.white70, size: 16),
        const SizedBox(width: 6),
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              label,
              style: const TextStyle(
                color: Colors.white54,
                fontSize: 11,
              ),
            ),
            Text(
              value,
              style: const TextStyle(
                color: AppColors.white,
                fontSize: 14,
                fontWeight: FontWeight.w500,
              ),
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildComingSoonSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          '더 많은 채널',
          style: TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: 12),
        _buildComingSoonCard(
          'Facebook Messenger',
          Icons.facebook,
          const Color(0xFF1877F2),
        ),
        const SizedBox(height: 8),
        _buildComingSoonCard(
          'KakaoTalk',
          Icons.chat_bubble,
          const Color(0xFFFEE500),
          textColor: Colors.black87,
        ),
        const SizedBox(height: 8),
        _buildComingSoonCard(
          'Naver TalkTalk',
          Icons.chat,
          const Color(0xFF03C75A),
        ),
      ],
    );
  }

  Widget _buildComingSoonCard(String name, IconData icon, Color color,
      {Color textColor = Colors.white}) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.grey50,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppColors.grey200),
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: color,
              borderRadius: BorderRadius.circular(8),
            ),
            child: Icon(icon, color: textColor, size: 20),
          ),
          const SizedBox(width: 12),
          Text(
            name,
            style: const TextStyle(
              fontWeight: FontWeight.w500,
            ),
          ),
          const Spacer(),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            decoration: BoxDecoration(
              color: AppColors.grey200,
              borderRadius: BorderRadius.circular(4),
            ),
            child: const Text(
              'Coming Soon',
              style: TextStyle(
                fontSize: 11,
                color: AppColors.grey600,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _showDisconnectDialog(int channelId) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('연결 해제'),
        content: const Text('정말로 Instagram 연결을 해제하시겠습니까?\n자동 응답이 중지됩니다.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('취소'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            style: TextButton.styleFrom(foregroundColor: AppColors.error),
            child: const Text('연결 해제'),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      await ref.read(channelsProvider.notifier).disconnectChannel(channelId);
    }
  }
}
