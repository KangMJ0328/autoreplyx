import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/theme/app_colors.dart';
import '../../../data/models/rule_model.dart';
import '../../providers/rules_provider.dart';
import '../../widgets/common/loading_overlay.dart';

class RuleFormScreen extends ConsumerStatefulWidget {
  final int? ruleId;

  const RuleFormScreen({super.key, this.ruleId});

  @override
  ConsumerState<RuleFormScreen> createState() => _RuleFormScreenState();
}

class _RuleFormScreenState extends ConsumerState<RuleFormScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _keywordsController = TextEditingController();
  final _responseTextController = TextEditingController();

  String _triggerType = 'keyword';
  String _responseType = 'ai';
  int _priority = 0;
  bool _isActive = true;
  bool _isLoading = false;

  bool get isEditing => widget.ruleId != null;

  @override
  void initState() {
    super.initState();
    if (isEditing) {
      _loadRule();
    }
  }

  Future<void> _loadRule() async {
    setState(() => _isLoading = true);
    try {
      final rule = await ref.read(selectedRuleProvider(widget.ruleId!).future);
      if (rule != null) {
        _nameController.text = rule.name;
        _triggerType = rule.triggerType;
        _keywordsController.text = rule.triggerKeywords ?? '';
        _responseType = rule.responseType;
        _responseTextController.text = rule.responseText ?? '';
        _priority = rule.priority;
        _isActive = rule.isActive;
        setState(() {});
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(e.toString())),
        );
      }
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  @override
  void dispose() {
    _nameController.dispose();
    _keywordsController.dispose();
    _responseTextController.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _isLoading = true);

    try {
      if (isEditing) {
        await ref.read(rulesProvider.notifier).updateRule(
          widget.ruleId!,
          {
            'name': _nameController.text,
            'triggerType': _triggerType,
            'triggerKeywords': _keywordsController.text.isEmpty
                ? null
                : _keywordsController.text,
            'responseType': _responseType,
            'responseText': _responseTextController.text.isEmpty
                ? null
                : _responseTextController.text,
            'priority': _priority,
            'isActive': _isActive,
          },
        );
      } else {
        await ref.read(rulesProvider.notifier).createRule(
          CreateRuleRequest(
            name: _nameController.text,
            triggerType: _triggerType,
            triggerKeywords: _keywordsController.text.isEmpty
                ? null
                : _keywordsController.text,
            responseType: _responseType,
            responseText: _responseTextController.text.isEmpty
                ? null
                : _responseTextController.text,
            priority: _priority,
            isActive: _isActive,
          ),
        );
      }

      if (mounted) {
        context.go('/rules');
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
        setState(() => _isLoading = false);
      }
    }
  }

  Future<void> _delete() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('규칙 삭제'),
        content: const Text('이 규칙을 삭제하시겠습니까?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('취소'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            style: TextButton.styleFrom(foregroundColor: AppColors.error),
            child: const Text('삭제'),
          ),
        ],
      ),
    );

    if (confirmed != true) return;

    setState(() => _isLoading = true);

    try {
      await ref.read(rulesProvider.notifier).deleteRule(widget.ruleId!);
      if (mounted) {
        context.go('/rules');
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(e.toString())),
        );
      }
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(isEditing ? '규칙 수정' : '새 규칙'),
        leading: IconButton(
          icon: const Icon(Icons.close),
          onPressed: () => context.go('/rules'),
        ),
        actions: [
          if (isEditing)
            IconButton(
              icon: const Icon(Icons.delete_outline),
              onPressed: _delete,
            ),
        ],
      ),
      body: LoadingOverlay(
        isLoading: _isLoading,
        child: Form(
          key: _formKey,
          child: ListView(
            padding: const EdgeInsets.all(16),
            children: [
              // Name
              TextFormField(
                controller: _nameController,
                decoration: const InputDecoration(
                  labelText: '규칙 이름 *',
                  hintText: '예: 영업시간 안내',
                ),
                validator: (value) {
                  if (value == null || value.isEmpty) {
                    return '규칙 이름을 입력해주세요';
                  }
                  return null;
                },
              ),
              const SizedBox(height: 24),

              // Trigger Type
              const Text(
                '트리거 타입',
                style: TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w500,
                  color: AppColors.grey700,
                ),
              ),
              const SizedBox(height: 8),
              Wrap(
                spacing: 8,
                children: [
                  _buildTriggerChip('keyword', '키워드'),
                  _buildTriggerChip('all', '모든 메시지'),
                  _buildTriggerChip('dm', 'DM'),
                  _buildTriggerChip('comment', '댓글'),
                  _buildTriggerChip('mention', '멘션'),
                ],
              ),
              const SizedBox(height: 16),

              // Keywords (if trigger type is keyword)
              if (_triggerType == 'keyword') ...[
                TextFormField(
                  controller: _keywordsController,
                  decoration: const InputDecoration(
                    labelText: '키워드',
                    hintText: '쉼표로 구분 (예: 가격, 문의, 예약)',
                  ),
                  maxLines: 2,
                ),
                const SizedBox(height: 16),
              ],

              // Response Type
              const Text(
                '응답 타입',
                style: TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w500,
                  color: AppColors.grey700,
                ),
              ),
              const SizedBox(height: 8),
              Row(
                children: [
                  Expanded(
                    child: _buildResponseTypeCard(
                      'ai',
                      'AI 응답',
                      Icons.smart_toy_outlined,
                      'AI가 상황에 맞는\n응답을 생성합니다',
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: _buildResponseTypeCard(
                      'fixed',
                      '고정 응답',
                      Icons.text_fields,
                      '설정한 텍스트로\n응답합니다',
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),

              // Response Text (if response type is fixed)
              if (_responseType == 'fixed') ...[
                TextFormField(
                  controller: _responseTextController,
                  decoration: const InputDecoration(
                    labelText: '응답 내용 *',
                    hintText: '자동으로 전송될 응답 메시지를 입력하세요',
                  ),
                  maxLines: 4,
                  validator: (value) {
                    if (_responseType == 'fixed' &&
                        (value == null || value.isEmpty)) {
                      return '응답 내용을 입력해주세요';
                    }
                    return null;
                  },
                ),
                const SizedBox(height: 16),
              ],

              // Priority
              Row(
                children: [
                  const Text(
                    '우선순위',
                    style: TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w500,
                      color: AppColors.grey700,
                    ),
                  ),
                  const Spacer(),
                  IconButton(
                    icon: const Icon(Icons.remove_circle_outline),
                    onPressed: _priority > 0
                        ? () => setState(() => _priority--)
                        : null,
                  ),
                  SizedBox(
                    width: 40,
                    child: Text(
                      '$_priority',
                      textAlign: TextAlign.center,
                      style: const TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                  IconButton(
                    icon: const Icon(Icons.add_circle_outline),
                    onPressed: () => setState(() => _priority++),
                  ),
                ],
              ),
              const SizedBox(height: 16),

              // Active toggle
              SwitchListTile(
                title: const Text('규칙 활성화'),
                subtitle: const Text('비활성화하면 이 규칙이 적용되지 않습니다'),
                value: _isActive,
                onChanged: (value) => setState(() => _isActive = value),
                activeColor: AppColors.primary,
              ),
              const SizedBox(height: 32),

              // Save button
              ElevatedButton(
                onPressed: _isLoading ? null : _save,
                child: Text(isEditing ? '저장' : '규칙 추가'),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildTriggerChip(String type, String label) {
    final isSelected = _triggerType == type;
    return FilterChip(
      label: Text(label),
      selected: isSelected,
      onSelected: (selected) {
        if (selected) {
          setState(() => _triggerType = type);
        }
      },
      selectedColor: AppColors.primaryLight.withOpacity(0.2),
      checkmarkColor: AppColors.primary,
    );
  }

  Widget _buildResponseTypeCard(
    String type,
    String title,
    IconData icon,
    String description,
  ) {
    final isSelected = _responseType == type;
    return InkWell(
      onTap: () => setState(() => _responseType = type),
      borderRadius: BorderRadius.circular(12),
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          border: Border.all(
            color: isSelected ? AppColors.primary : AppColors.grey200,
            width: isSelected ? 2 : 1,
          ),
          borderRadius: BorderRadius.circular(12),
          color: isSelected
              ? AppColors.primaryLight.withOpacity(0.1)
              : AppColors.white,
        ),
        child: Column(
          children: [
            Icon(
              icon,
              size: 32,
              color: isSelected ? AppColors.primary : AppColors.grey400,
            ),
            const SizedBox(height: 8),
            Text(
              title,
              style: TextStyle(
                fontWeight: FontWeight.w600,
                color: isSelected ? AppColors.primary : AppColors.grey700,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              description,
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontSize: 11,
                color: AppColors.grey500,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
