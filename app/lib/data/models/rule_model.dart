class RuleModel {
  final int id;
  final String name;
  final String triggerType;
  final String? triggerKeywords;
  final String responseType;
  final String? responseText;
  final int priority;
  final bool isActive;
  final String createdAt;
  final String? updatedAt;

  RuleModel({
    required this.id,
    required this.name,
    required this.triggerType,
    this.triggerKeywords,
    required this.responseType,
    this.responseText,
    required this.priority,
    required this.isActive,
    required this.createdAt,
    this.updatedAt,
  });

  factory RuleModel.fromJson(Map<String, dynamic> json) {
    return RuleModel(
      id: json['id'] as int,
      name: json['name'] as String,
      triggerType: json['triggerType'] as String,
      triggerKeywords: json['triggerKeywords'] as String?,
      responseType: json['responseType'] as String,
      responseText: json['responseText'] as String?,
      priority: json['priority'] as int? ?? 0,
      isActive: json['isActive'] as bool? ?? true,
      createdAt: json['createdAt'] as String? ?? '',
      updatedAt: json['updatedAt'] as String?,
    );
  }

  Map<String, dynamic> toJson() => {
    'id': id,
    'name': name,
    'triggerType': triggerType,
    'triggerKeywords': triggerKeywords,
    'responseType': responseType,
    'responseText': responseText,
    'priority': priority,
    'isActive': isActive,
    'createdAt': createdAt,
    'updatedAt': updatedAt,
  };

  RuleModel copyWith({
    int? id,
    String? name,
    String? triggerType,
    String? triggerKeywords,
    String? responseType,
    String? responseText,
    int? priority,
    bool? isActive,
    String? createdAt,
    String? updatedAt,
  }) {
    return RuleModel(
      id: id ?? this.id,
      name: name ?? this.name,
      triggerType: triggerType ?? this.triggerType,
      triggerKeywords: triggerKeywords ?? this.triggerKeywords,
      responseType: responseType ?? this.responseType,
      responseText: responseText ?? this.responseText,
      priority: priority ?? this.priority,
      isActive: isActive ?? this.isActive,
      createdAt: createdAt ?? this.createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
    );
  }
}

class CreateRuleRequest {
  final String name;
  final String triggerType;
  final String? triggerKeywords;
  final String responseType;
  final String? responseText;
  final int priority;
  final bool isActive;

  CreateRuleRequest({
    required this.name,
    required this.triggerType,
    this.triggerKeywords,
    required this.responseType,
    this.responseText,
    this.priority = 0,
    this.isActive = true,
  });

  Map<String, dynamic> toJson() => {
    'name': name,
    'triggerType': triggerType,
    'triggerKeywords': triggerKeywords,
    'responseType': responseType,
    'responseText': responseText,
    'priority': priority,
    'isActive': isActive,
  };
}
