import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Mail, Lock, Building2, MessageSquare, Coffee, ShoppingBag, Scissors, Briefcase, Utensils, Building, Loader2 } from 'lucide-react';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { useAuth } from '../contexts/AuthContext';

export default function Signup() {
  const navigate = useNavigate();
  const { register } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [passwordConfirm, setPasswordConfirm] = useState('');
  const [brandName, setBrandName] = useState('');
  const [industry, setIndustry] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const industries = [
    { id: 'cafe', label: '카페', icon: Coffee, emoji: '☕' },
    { id: 'shopping', label: '쇼핑몰', icon: ShoppingBag, emoji: '🛍️' },
    { id: 'beauty', label: '미용실', icon: Scissors, emoji: '💅' },
    { id: 'freelance', label: '프리랜서', icon: Briefcase, emoji: '💼' },
    { id: 'restaurant', label: '음식점', icon: Utensils, emoji: '🍽️' },
    { id: 'other', label: '기타', icon: Building, emoji: '🏢' },
  ];

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (password !== passwordConfirm) {
      setError('비밀번호가 일치하지 않습니다.');
      return;
    }

    if (password.length < 8) {
      setError('비밀번호는 8자 이상이어야 합니다.');
      return;
    }

    if (!industry) {
      setError('업종을 선택해주세요.');
      return;
    }

    setIsLoading(true);

    try {
      await register({
        email,
        password,
        password_confirmation: passwordConfirm,
        brand_name: brandName,
        industry,
      });
      navigate('/onboarding');
    } catch (err: any) {
      // Extract error message from various response formats
      const errorData = err.response?.data;
      let errorMessage = '회원가입에 실패했습니다. 다시 시도해주세요.';

      if (errorData) {
        if (typeof errorData === 'string') {
          errorMessage = errorData;
        } else if (errorData.message) {
          errorMessage = errorData.message;
        } else if (errorData.error) {
          errorMessage = errorData.error;
        } else if (errorData.errors) {
          // Handle validation errors object
          const errors = Object.values(errorData.errors).flat();
          if (errors.length > 0) {
            errorMessage = errors.join(', ');
          }
        }
      }

      // Add HTTP status info for debugging
      if (err.response?.status === 400) {
        console.error('Registration failed:', errorData);
      }

      setError(errorMessage);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-purple-50 to-pink-50 flex items-center justify-center p-4">
      <div className="w-full max-w-2xl">
        <div className="bg-white rounded-2xl shadow-xl p-8">
          {/* Logo */}
          <div className="flex justify-center mb-6">
            <div className="w-16 h-16 bg-gradient-to-br from-blue-500 to-purple-600 rounded-2xl flex items-center justify-center">
              <MessageSquare className="w-10 h-10 text-white" />
            </div>
          </div>

          {/* Title */}
          <div className="text-center mb-8">
            <h1 className="text-gray-900 mb-2">시작하기</h1>
            <p className="text-gray-600">AutoReplyX로 자동응답을 시작하세요</p>
          </div>

          {/* Error Message */}
          {error && (
            <div className="mb-6 p-3 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm">
              {error}
            </div>
          )}

          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <Label htmlFor="email">이메일</Label>
              <div className="relative mt-1.5">
                <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                <Input
                  id="email"
                  type="email"
                  placeholder="example@email.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="pl-10"
                  required
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label htmlFor="password">비밀번호</Label>
                <div className="relative mt-1.5">
                  <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                  <Input
                    id="password"
                    type="password"
                    placeholder="••••••••"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="pl-10"
                    required
                  />
                </div>
              </div>
              <div>
                <Label htmlFor="passwordConfirm">비밀번호 확인</Label>
                <div className="relative mt-1.5">
                  <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                  <Input
                    id="passwordConfirm"
                    type="password"
                    placeholder="••••••••"
                    value={passwordConfirm}
                    onChange={(e) => setPasswordConfirm(e.target.value)}
                    className="pl-10"
                    required
                  />
                </div>
              </div>
            </div>

            <div>
              <Label htmlFor="brandName">브랜드명</Label>
              <div className="relative mt-1.5">
                <Building2 className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                <Input
                  id="brandName"
                  type="text"
                  placeholder="브랜드 이름을 입력하세요"
                  value={brandName}
                  onChange={(e) => setBrandName(e.target.value)}
                  className="pl-10"
                  required
                />
              </div>
            </div>

            <div>
              <Label>업종 선택</Label>
              <div className="grid grid-cols-3 gap-3 mt-1.5">
                {industries.map((item) => {
                  const Icon = item.icon;
                  return (
                    <button
                      key={item.id}
                      type="button"
                      onClick={() => setIndustry(item.id)}
                      className={`p-4 rounded-lg border-2 transition-all ${
                        industry === item.id
                          ? 'border-blue-500 bg-blue-50'
                          : 'border-gray-200 hover:border-gray-300'
                      }`}
                    >
                      <div className="flex flex-col items-center gap-2">
                        <span className="text-2xl">{item.emoji}</span>
                        <span className="text-gray-700">{item.label}</span>
                      </div>
                    </button>
                  );
                })}
              </div>
            </div>

            <Button
              type="submit"
              disabled={isLoading}
              className="w-full bg-gradient-to-r from-blue-500 to-purple-600 hover:from-blue-600 hover:to-purple-700"
            >
              {isLoading ? (
                <>
                  <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                  가입 중...
                </>
              ) : (
                '회원가입'
              )}
            </Button>
          </form>

          {/* Login Link */}
          <div className="mt-6 text-center">
            <p className="text-gray-600">
              이미 계정이 있으신가요?{' '}
              <Link to="/login" className="text-blue-600 hover:text-blue-700">
                로그인
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
