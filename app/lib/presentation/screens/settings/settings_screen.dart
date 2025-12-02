import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/theme/app_colors.dart';
import '../../providers/auth_provider.dart';
import '../../widgets/common/loading_overlay.dart';

class SettingsScreen extends ConsumerStatefulWidget {
  const SettingsScreen({super.key});

  @override
  ConsumerState<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends ConsumerState<SettingsScreen> {
  bool _isLoading = false;

  @override
  Widget build(BuildContext context) {
    final user = ref.watch(currentUserProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('설정'),
      ),
      body: LoadingOverlay(
        isLoading: _isLoading,
        child: ListView(
          children: [
            // Profile section
            Container(
              padding: const EdgeInsets.all(20),
              child: Row(
                children: [
                  CircleAvatar(
                    radius: 32,
                    backgroundColor: AppColors.primary,
                    child: Text(
                      (user?.brandName ?? 'A')[0].toUpperCase(),
                      style: const TextStyle(
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                        color: AppColors.white,
                      ),
                    ),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          user?.brandName ?? '',
                          style: const TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        Text(
                          user?.email ?? '',
                          style: const TextStyle(
                            color: AppColors.grey500,
                          ),
                        ),
                      ],
                    ),
                  ),
                  IconButton(
                    icon: const Icon(Icons.edit_outlined),
                    onPressed: () => _showEditProfileSheet(context),
                  ),
                ],
              ),
            ),
            const Divider(),

            // Subscription
            _buildSettingsItem(
              icon: Icons.card_membership,
              title: '구독 관리',
              subtitle: '${user?.plan ?? 'Free'} 플랜',
              trailing: Container(
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
              onTap: () => context.go('/subscription'),
            ),

            const SizedBox(height: 8),
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              child: Text(
                '비즈니스 설정',
                style: TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.w500,
                  color: AppColors.grey500,
                ),
              ),
            ),

            // Business info
            _buildSettingsItem(
              icon: Icons.store,
              title: '비즈니스 정보',
              subtitle: '영업시간, 주소, 연락처',
              onTap: () => _showBusinessInfoSheet(context),
            ),

            // AI settings
            _buildSettingsItem(
              icon: Icons.smart_toy,
              title: 'AI 응답 설정',
              subtitle: '응답 톤, 금지어',
              onTap: () => _showAISettingsSheet(context),
            ),

            const SizedBox(height: 8),
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              child: Text(
                '앱 설정',
                style: TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.w500,
                  color: AppColors.grey500,
                ),
              ),
            ),

            // Notifications
            _buildSettingsItem(
              icon: Icons.notifications_outlined,
              title: '알림 설정',
              subtitle: '푸시 알림 관리',
              onTap: () {},
            ),

            // Help
            _buildSettingsItem(
              icon: Icons.help_outline,
              title: '도움말',
              subtitle: '사용 가이드 및 FAQ',
              onTap: () {},
            ),

            // Terms
            _buildSettingsItem(
              icon: Icons.description_outlined,
              title: '이용약관',
              onTap: () {},
            ),

            // Privacy
            _buildSettingsItem(
              icon: Icons.privacy_tip_outlined,
              title: '개인정보 처리방침',
              onTap: () {},
            ),

            const SizedBox(height: 24),

            // Logout
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: OutlinedButton(
                onPressed: _logout,
                style: OutlinedButton.styleFrom(
                  foregroundColor: AppColors.error,
                  side: const BorderSide(color: AppColors.error),
                ),
                child: const Text('로그아웃'),
              ),
            ),

            const SizedBox(height: 24),

            // Version info
            Center(
              child: Text(
                'AutoReplyX v1.0.0',
                style: TextStyle(
                  fontSize: 12,
                  color: AppColors.grey400,
                ),
              ),
            ),
            const SizedBox(height: 32),
          ],
        ),
      ),
    );
  }

  Widget _buildSettingsItem({
    required IconData icon,
    required String title,
    String? subtitle,
    Widget? trailing,
    required VoidCallback onTap,
  }) {
    return ListTile(
      leading: Container(
        padding: const EdgeInsets.all(8),
        decoration: BoxDecoration(
          color: AppColors.grey100,
          borderRadius: BorderRadius.circular(8),
        ),
        child: Icon(icon, color: AppColors.grey700, size: 20),
      ),
      title: Text(title),
      subtitle: subtitle != null
          ? Text(
              subtitle,
              style: const TextStyle(
                fontSize: 13,
                color: AppColors.grey500,
              ),
            )
          : null,
      trailing: trailing ?? const Icon(Icons.chevron_right, color: AppColors.grey400),
      onTap: onTap,
    );
  }

  Future<void> _logout() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('로그아웃'),
        content: const Text('정말 로그아웃 하시겠습니까?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('취소'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            style: TextButton.styleFrom(foregroundColor: AppColors.error),
            child: const Text('로그아웃'),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      setState(() => _isLoading = true);
      await ref.read(authStateProvider.notifier).logout();
      if (mounted) {
        context.go('/auth/login');
      }
    }
  }

  void _showEditProfileSheet(BuildContext context) {
    final user = ref.read(currentUserProvider);
    final brandNameController = TextEditingController(text: user?.brandName);

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) => Padding(
        padding: EdgeInsets.only(
          left: 20,
          right: 20,
          top: 20,
          bottom: MediaQuery.of(context).viewInsets.bottom + 20,
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '프로필 수정',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 20),
            TextField(
              controller: brandNameController,
              decoration: const InputDecoration(
                labelText: '브랜드명',
              ),
            ),
            const SizedBox(height: 20),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: () async {
                  await ref.read(authStateProvider.notifier).updateProfile({
                    'brandName': brandNameController.text,
                  });
                  if (context.mounted) Navigator.pop(context);
                },
                child: const Text('저장'),
              ),
            ),
          ],
        ),
      ),
    );
  }

  void _showBusinessInfoSheet(BuildContext context) {
    final user = ref.read(currentUserProvider);
    final hoursController = TextEditingController(text: user?.businessHours);
    final addressController = TextEditingController(text: user?.address);
    final phoneController = TextEditingController(text: user?.phone);
    final descController = TextEditingController(text: user?.description);

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) => Padding(
        padding: EdgeInsets.only(
          left: 20,
          right: 20,
          top: 20,
          bottom: MediaQuery.of(context).viewInsets.bottom + 20,
        ),
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                '비즈니스 정보',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 8),
              const Text(
                'AI가 응답할 때 이 정보를 참고합니다.',
                style: TextStyle(
                  color: AppColors.grey500,
                  fontSize: 13,
                ),
              ),
              const SizedBox(height: 20),
              TextField(
                controller: hoursController,
                decoration: const InputDecoration(
                  labelText: '영업시간',
                  hintText: '예: 평일 09:00-18:00',
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: addressController,
                decoration: const InputDecoration(
                  labelText: '주소',
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: phoneController,
                decoration: const InputDecoration(
                  labelText: '연락처',
                ),
                keyboardType: TextInputType.phone,
              ),
              const SizedBox(height: 12),
              TextField(
                controller: descController,
                decoration: const InputDecoration(
                  labelText: '비즈니스 소개',
                  hintText: 'AI가 참고할 비즈니스 설명',
                ),
                maxLines: 3,
              ),
              const SizedBox(height: 20),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: () async {
                    await ref.read(authStateProvider.notifier).updateProfile({
                      'businessHours': hoursController.text,
                      'address': addressController.text,
                      'phone': phoneController.text,
                      'description': descController.text,
                    });
                    if (context.mounted) Navigator.pop(context);
                  },
                  child: const Text('저장'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _showAISettingsSheet(BuildContext context) {
    final user = ref.read(currentUserProvider);
    String selectedTone = user?.aiTone ?? 'friendly';
    final bannedWordsController = TextEditingController(text: user?.bannedWords);

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) => StatefulBuilder(
        builder: (context, setSheetState) => Padding(
          padding: EdgeInsets.only(
            left: 20,
            right: 20,
            top: 20,
            bottom: MediaQuery.of(context).viewInsets.bottom + 20,
          ),
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'AI 응답 설정',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 20),
                const Text(
                  '응답 톤',
                  style: TextStyle(fontWeight: FontWeight.w500),
                ),
                const SizedBox(height: 8),
                Wrap(
                  spacing: 8,
                  children: [
                    _buildToneChip('friendly', '친근한', selectedTone, (tone) {
                      setSheetState(() => selectedTone = tone);
                    }),
                    _buildToneChip('professional', '전문적인', selectedTone, (tone) {
                      setSheetState(() => selectedTone = tone);
                    }),
                    _buildToneChip('formal', '격식있는', selectedTone, (tone) {
                      setSheetState(() => selectedTone = tone);
                    }),
                    _buildToneChip('casual', '캐주얼한', selectedTone, (tone) {
                      setSheetState(() => selectedTone = tone);
                    }),
                  ],
                ),
                const SizedBox(height: 20),
                TextField(
                  controller: bannedWordsController,
                  decoration: const InputDecoration(
                    labelText: '금지어',
                    hintText: '쉼표로 구분 (예: 경쟁사, 할인)',
                  ),
                  maxLines: 2,
                ),
                const SizedBox(height: 8),
                const Text(
                  'AI 응답에서 제외할 단어들을 입력하세요.',
                  style: TextStyle(
                    fontSize: 12,
                    color: AppColors.grey500,
                  ),
                ),
                const SizedBox(height: 20),
                SizedBox(
                  width: double.infinity,
                  child: ElevatedButton(
                    onPressed: () async {
                      await ref.read(authStateProvider.notifier).updateProfile({
                        'aiTone': selectedTone,
                        'bannedWords': bannedWordsController.text,
                      });
                      if (context.mounted) Navigator.pop(context);
                    },
                    child: const Text('저장'),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildToneChip(
    String value,
    String label,
    String selected,
    Function(String) onSelected,
  ) {
    return FilterChip(
      label: Text(label),
      selected: selected == value,
      onSelected: (_) => onSelected(value),
      selectedColor: AppColors.primaryLight.withOpacity(0.2),
      checkmarkColor: AppColors.primary,
    );
  }
}
