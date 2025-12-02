import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/theme/app_colors.dart';
import '../../providers/rules_provider.dart';
import '../../widgets/common/loading_overlay.dart';
import '../../widgets/common/error_view.dart';

class RulesScreen extends ConsumerWidget {
  const RulesScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final rulesAsync = ref.watch(rulesProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('응답 규칙'),
        actions: [
          IconButton(
            icon: const Icon(Icons.help_outline),
            onPressed: () => _showHelpDialog(context),
          ),
        ],
      ),
      body: rulesAsync.when(
        data: (rules) {
          if (rules.isEmpty) {
            return EmptyView(
              message: '등록된 규칙이 없습니다\n새 규칙을 추가해보세요',
              icon: Icons.rule_outlined,
              action: ElevatedButton.icon(
                onPressed: () => context.go('/rules/new'),
                icon: const Icon(Icons.add),
                label: const Text('규칙 추가'),
              ),
            );
          }

          return RefreshIndicator(
            onRefresh: () => ref.read(rulesProvider.notifier).refresh(),
            child: ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: rules.length,
              itemBuilder: (context, index) {
                final rule = rules[index];
                return _buildRuleCard(context, ref, rule);
              },
            ),
          );
        },
        loading: () => const LoadingWidget(),
        error: (e, _) => ErrorView(
          message: e.toString(),
          onRetry: () => ref.read(rulesProvider.notifier).refresh(),
        ),
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => context.go('/rules/new'),
        icon: const Icon(Icons.add),
        label: const Text('규칙 추가'),
      ),
    );
  }

  Widget _buildRuleCard(BuildContext context, WidgetRef ref, rule) {
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: InkWell(
        onTap: () => context.go('/rules/${rule.id}'),
        borderRadius: BorderRadius.circular(16),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 8,
                      vertical: 4,
                    ),
                    decoration: BoxDecoration(
                      color: _getTriggerTypeColor(rule.triggerType)
                          .withOpacity(0.1),
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: Text(
                      _getTriggerTypeLabel(rule.triggerType),
                      style: TextStyle(
                        fontSize: 12,
                        color: _getTriggerTypeColor(rule.triggerType),
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ),
                  const Spacer(),
                  Switch(
                    value: rule.isActive,
                    onChanged: (value) {
                      ref
                          .read(rulesProvider.notifier)
                          .toggleRule(rule.id, value);
                    },
                    activeColor: AppColors.primary,
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Text(
                rule.name,
                style: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                ),
              ),
              if (rule.triggerKeywords != null) ...[
                const SizedBox(height: 8),
                Text(
                  '키워드: ${rule.triggerKeywords}',
                  style: const TextStyle(
                    fontSize: 14,
                    color: AppColors.grey500,
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
              const SizedBox(height: 8),
              Row(
                children: [
                  Icon(
                    rule.responseType == 'ai'
                        ? Icons.smart_toy_outlined
                        : Icons.text_fields,
                    size: 16,
                    color: AppColors.grey400,
                  ),
                  const SizedBox(width: 4),
                  Text(
                    rule.responseType == 'ai' ? 'AI 응답' : '고정 응답',
                    style: const TextStyle(
                      fontSize: 12,
                      color: AppColors.grey400,
                    ),
                  ),
                  const Spacer(),
                  Text(
                    '우선순위: ${rule.priority}',
                    style: const TextStyle(
                      fontSize: 12,
                      color: AppColors.grey400,
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  String _getTriggerTypeLabel(String type) {
    switch (type) {
      case 'keyword':
        return '키워드';
      case 'all':
        return '모든 메시지';
      case 'dm':
        return 'DM';
      case 'comment':
        return '댓글';
      case 'mention':
        return '멘션';
      default:
        return type;
    }
  }

  Color _getTriggerTypeColor(String type) {
    switch (type) {
      case 'keyword':
        return AppColors.info;
      case 'all':
        return AppColors.secondary;
      case 'dm':
        return AppColors.primary;
      case 'comment':
        return AppColors.accent;
      case 'mention':
        return AppColors.error;
      default:
        return AppColors.grey500;
    }
  }

  void _showHelpDialog(BuildContext context) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('규칙 사용 안내'),
        content: const SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                '트리거 타입',
                style: TextStyle(fontWeight: FontWeight.bold),
              ),
              SizedBox(height: 8),
              Text('- 키워드: 특정 단어가 포함된 메시지에 응답'),
              Text('- 모든 메시지: 모든 수신 메시지에 응답'),
              Text('- DM: DM 메시지에만 응답'),
              Text('- 댓글: 게시물 댓글에만 응답'),
              Text('- 멘션: 멘션이 포함된 메시지에 응답'),
              SizedBox(height: 16),
              Text(
                '응답 타입',
                style: TextStyle(fontWeight: FontWeight.bold),
              ),
              SizedBox(height: 8),
              Text('- AI 응답: AI가 상황에 맞는 응답을 생성'),
              Text('- 고정 응답: 설정한 텍스트로 응답'),
              SizedBox(height: 16),
              Text(
                '우선순위',
                style: TextStyle(fontWeight: FontWeight.bold),
              ),
              SizedBox(height: 8),
              Text('숫자가 높을수록 먼저 적용됩니다.'),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('확인'),
          ),
        ],
      ),
    );
  }
}
