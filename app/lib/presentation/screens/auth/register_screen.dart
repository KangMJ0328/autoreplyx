import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/theme/app_colors.dart';
import '../../providers/auth_provider.dart';
import '../../widgets/common/loading_overlay.dart';

class RegisterScreen extends ConsumerStatefulWidget {
  const RegisterScreen({super.key});

  @override
  ConsumerState<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends ConsumerState<RegisterScreen> {
  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _confirmPasswordController = TextEditingController();
  final _brandNameController = TextEditingController();
  bool _isLoading = false;
  bool _obscurePassword = true;
  bool _obscureConfirmPassword = true;
  String? _selectedIndustry;

  final List<String> _industries = [
    '음식점/카페',
    '뷰티/헬스',
    '패션/의류',
    '교육/학원',
    '의료/병원',
    '부동산',
    '숙박/여행',
    '자동차',
    '금융/보험',
    '기타',
  ];

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    _confirmPasswordController.dispose();
    _brandNameController.dispose();
    super.dispose();
  }

  Future<void> _register() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _isLoading = true);

    try {
      await ref.read(authStateProvider.notifier).register(
            email: _emailController.text.trim(),
            password: _passwordController.text,
            brandName: _brandNameController.text.trim(),
            industry: _selectedIndustry,
          );
      if (mounted) {
        context.go('/dashboard');
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

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.go('/auth/login'),
        ),
        title: const Text('회원가입'),
      ),
      body: LoadingOverlay(
        isLoading: _isLoading,
        child: SafeArea(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: Form(
              key: _formKey,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Text(
                    '비즈니스 정보를 입력해주세요',
                    style: TextStyle(
                      fontSize: 16,
                      color: AppColors.grey600,
                    ),
                  ),
                  const SizedBox(height: 24),
                  // Brand name field
                  TextFormField(
                    controller: _brandNameController,
                    decoration: const InputDecoration(
                      labelText: '브랜드명 *',
                      hintText: '고객에게 표시될 이름',
                      prefixIcon: Icon(Icons.store_outlined),
                    ),
                    validator: (value) {
                      if (value == null || value.isEmpty) {
                        return '브랜드명을 입력해주세요';
                      }
                      return null;
                    },
                  ),
                  const SizedBox(height: 16),
                  // Industry dropdown
                  DropdownButtonFormField<String>(
                    value: _selectedIndustry,
                    decoration: const InputDecoration(
                      labelText: '업종',
                      prefixIcon: Icon(Icons.category_outlined),
                    ),
                    items: _industries.map((industry) {
                      return DropdownMenuItem(
                        value: industry,
                        child: Text(industry),
                      );
                    }).toList(),
                    onChanged: (value) {
                      setState(() => _selectedIndustry = value);
                    },
                  ),
                  const SizedBox(height: 16),
                  // Email field
                  TextFormField(
                    controller: _emailController,
                    keyboardType: TextInputType.emailAddress,
                    decoration: const InputDecoration(
                      labelText: '이메일 *',
                      hintText: 'example@email.com',
                      prefixIcon: Icon(Icons.email_outlined),
                    ),
                    validator: (value) {
                      if (value == null || value.isEmpty) {
                        return '이메일을 입력해주세요';
                      }
                      if (!RegExp(r'^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$')
                          .hasMatch(value)) {
                        return '올바른 이메일 형식이 아닙니다';
                      }
                      return null;
                    },
                  ),
                  const SizedBox(height: 16),
                  // Password field
                  TextFormField(
                    controller: _passwordController,
                    obscureText: _obscurePassword,
                    decoration: InputDecoration(
                      labelText: '비밀번호 *',
                      hintText: '8자 이상',
                      prefixIcon: const Icon(Icons.lock_outlined),
                      suffixIcon: IconButton(
                        icon: Icon(
                          _obscurePassword
                              ? Icons.visibility_outlined
                              : Icons.visibility_off_outlined,
                        ),
                        onPressed: () {
                          setState(() => _obscurePassword = !_obscurePassword);
                        },
                      ),
                    ),
                    validator: (value) {
                      if (value == null || value.isEmpty) {
                        return '비밀번호를 입력해주세요';
                      }
                      if (value.length < 8) {
                        return '비밀번호는 8자 이상이어야 합니다';
                      }
                      return null;
                    },
                  ),
                  const SizedBox(height: 16),
                  // Confirm password field
                  TextFormField(
                    controller: _confirmPasswordController,
                    obscureText: _obscureConfirmPassword,
                    decoration: InputDecoration(
                      labelText: '비밀번호 확인 *',
                      hintText: '비밀번호를 다시 입력해주세요',
                      prefixIcon: const Icon(Icons.lock_outlined),
                      suffixIcon: IconButton(
                        icon: Icon(
                          _obscureConfirmPassword
                              ? Icons.visibility_outlined
                              : Icons.visibility_off_outlined,
                        ),
                        onPressed: () {
                          setState(() =>
                              _obscureConfirmPassword = !_obscureConfirmPassword);
                        },
                      ),
                    ),
                    validator: (value) {
                      if (value == null || value.isEmpty) {
                        return '비밀번호 확인을 입력해주세요';
                      }
                      if (value != _passwordController.text) {
                        return '비밀번호가 일치하지 않습니다';
                      }
                      return null;
                    },
                  ),
                  const SizedBox(height: 32),
                  // Register button
                  ElevatedButton(
                    onPressed: _isLoading ? null : _register,
                    child: const Text('회원가입'),
                  ),
                  const SizedBox(height: 16),
                  // Login link
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      const Text(
                        '이미 계정이 있으신가요?',
                        style: TextStyle(color: AppColors.grey500),
                      ),
                      TextButton(
                        onPressed: () => context.go('/auth/login'),
                        child: const Text('로그인'),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
