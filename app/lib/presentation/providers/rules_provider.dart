import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/datasources/remote/rule_remote_datasource.dart';
import '../../data/models/rule_model.dart';

final rulesProvider = AsyncNotifierProvider<RulesNotifier, List<RuleModel>>(() {
  return RulesNotifier();
});

class RulesNotifier extends AsyncNotifier<List<RuleModel>> {
  @override
  Future<List<RuleModel>> build() async {
    final dataSource = ref.watch(ruleRemoteDataSourceProvider);
    return dataSource.getRules();
  }

  Future<void> refresh() async {
    state = const AsyncValue.loading();
    state = await AsyncValue.guard(() async {
      final dataSource = ref.read(ruleRemoteDataSourceProvider);
      return dataSource.getRules();
    });
  }

  Future<RuleModel> createRule(CreateRuleRequest request) async {
    final dataSource = ref.read(ruleRemoteDataSourceProvider);
    final rule = await dataSource.createRule(request);

    final currentRules = state.valueOrNull ?? [];
    state = AsyncValue.data([...currentRules, rule]);

    return rule;
  }

  Future<RuleModel> updateRule(int id, Map<String, dynamic> data) async {
    final dataSource = ref.read(ruleRemoteDataSourceProvider);
    final updatedRule = await dataSource.updateRule(id, data);

    final currentRules = state.valueOrNull ?? [];
    state = AsyncValue.data(
      currentRules.map((r) => r.id == id ? updatedRule : r).toList(),
    );

    return updatedRule;
  }

  Future<void> deleteRule(int id) async {
    final dataSource = ref.read(ruleRemoteDataSourceProvider);
    await dataSource.deleteRule(id);

    final currentRules = state.valueOrNull ?? [];
    state = AsyncValue.data(
      currentRules.where((r) => r.id != id).toList(),
    );
  }

  Future<void> toggleRule(int id, bool isActive) async {
    final dataSource = ref.read(ruleRemoteDataSourceProvider);
    final updatedRule = await dataSource.toggleRule(id, isActive);

    final currentRules = state.valueOrNull ?? [];
    state = AsyncValue.data(
      currentRules.map((r) => r.id == id ? updatedRule : r).toList(),
    );
  }
}

// Selected rule for editing
final selectedRuleProvider = FutureProvider.family<RuleModel?, int>((ref, id) async {
  if (id == 0) return null;
  final dataSource = ref.watch(ruleRemoteDataSourceProvider);
  return dataSource.getRule(id);
});
