import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_colors.dart';
import '../../widgets/common/error_view.dart';

class ReservationsScreen extends ConsumerWidget {
  const ReservationsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('예약 관리'),
      ),
      body: const EmptyView(
        message: '예약 기능은 준비 중입니다',
        icon: Icons.calendar_today_outlined,
      ),
    );
  }
}
