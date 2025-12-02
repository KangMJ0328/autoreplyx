import { Outlet, Link, useLocation, useNavigate } from 'react-router-dom';
import { LayoutDashboard, MessageSquare, Link2, Calendar, FileText, User, LogOut, Crown, Send } from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';

export default function Layout() {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuth();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const navItems = [
    { path: '/dashboard', icon: LayoutDashboard, label: '대시보드' },
    { path: '/rules', icon: MessageSquare, label: '자동응답 규칙' },
    { path: '/channels', icon: Link2, label: '채널 연동' },
    { path: '/reservations', icon: Calendar, label: '예약 관리' },
    { path: '/logs', icon: FileText, label: '메시지 로그' },
    { path: '/profile', icon: User, label: '프로필 설정' },
    { path: '/subscription', icon: Crown, label: '구독 관리' },
    { path: '/test', icon: Send, label: '메시지 테스트' },
  ];

  return (
    <div className="flex h-screen bg-gray-50">
      {/* Sidebar */}
      <aside className="w-64 bg-white border-r border-gray-200 flex flex-col">
        <div className="p-6 border-b border-gray-200">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-gradient-to-br from-blue-500 to-purple-600 rounded-lg flex items-center justify-center">
              <MessageSquare className="w-6 h-6 text-white" />
            </div>
            <div>
              <h1 className="text-gray-900">AutoReplyX</h1>
              <p className="text-xs text-gray-500">AI 자동응답</p>
            </div>
          </div>
        </div>

        <nav className="flex-1 p-4">
          <ul className="space-y-1">
            {navItems.map((item) => {
              const Icon = item.icon;
              const isActive = location.pathname === item.path;
              return (
                <li key={item.path}>
                  <Link
                    to={item.path}
                    className={`flex items-center gap-3 px-4 py-3 rounded-lg transition-colors ${
                      isActive
                        ? 'bg-blue-50 text-blue-600'
                        : 'text-gray-700 hover:bg-gray-50'
                    }`}
                  >
                    <Icon className="w-5 h-5" />
                    <span>{item.label}</span>
                  </Link>
                </li>
              );
            })}
          </ul>
        </nav>

        <div className="p-4 border-t border-gray-200">
          {user && (
            <div className="px-4 py-2 mb-2 text-sm text-gray-600">
              <p className="font-medium text-gray-900">{user.brand_name || user.email}</p>
              <p className="text-xs">{user.email}</p>
            </div>
          )}
          <button
            onClick={handleLogout}
            className="flex items-center gap-3 px-4 py-3 rounded-lg text-gray-700 hover:bg-gray-50 w-full transition-colors"
          >
            <LogOut className="w-5 h-5" />
            <span>로그아웃</span>
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 overflow-auto">
        <Outlet />
      </main>
    </div>
  );
}
