import { 
  Plus, 
  HardDrive, 
  Users, 
  Clock, 
  Star, 
  Trash2, 
  Cloud,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useDriveStore } from '@/stores/driveStore';
import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { cn } from '@/lib/utils';

// Updated: Added light mode defaults and moved existing styles behind `dark:` prefix
const PLAN_THEMES = {
  DEFAULT: {
    accent: 'bg-gray-50 border-gray-200 dark:bg-white/5 dark:border-white/10',
    text: 'text-gray-800 dark:text-white/80',
    button: 'bg-gray-200 text-gray-900 hover:bg-gray-300 dark:bg-gradient-to-r dark:from-gray-600 dark:to-gray-700 dark:text-white',
    progress: 'bg-gray-400 dark:bg-gradient-to-r dark:from-gray-400 dark:to-gray-500',
  },
  BASIC: {
    accent: 'bg-blue-50 border-blue-200 dark:bg-gradient-to-r dark:from-blue-700/20 dark:to-blue-900/20 dark:border-blue-500/30',
    text: 'text-blue-700 dark:text-blue-200',
    button: 'bg-blue-600 text-white hover:bg-blue-700 dark:bg-gradient-to-r dark:from-blue-600 dark:to-blue-700',
    progress: 'bg-blue-500 dark:bg-gradient-to-r dark:from-blue-400 dark:to-blue-600',
  },
  PRO: {
    accent: 'bg-purple-50 border-purple-200 dark:bg-gradient-to-r dark:from-purple-700/20 dark:to-pink-900/20 dark:border-purple-500/30',
    text: 'text-purple-700 dark:text-purple-200',
    button: 'bg-purple-600 text-white hover:bg-purple-700 dark:bg-gradient-to-r dark:from-purple-600 dark:to-pink-600',
    progress: 'bg-purple-500 dark:bg-gradient-to-r dark:from-indigo-400 dark:to-pink-500',
  },
  TEAM: {
    accent: 'bg-rose-50 border-rose-200 dark:bg-gradient-to-r dark:from-rose-700/20 dark:to-orange-900/20 dark:border-rose-500/30',
    text: 'text-rose-700 dark:text-rose-200',
    button: 'bg-rose-600 text-white hover:bg-rose-700 dark:bg-gradient-to-r dark:from-rose-600 dark:to-orange-600',
    progress: 'bg-rose-500 dark:bg-gradient-to-r dark:from-rose-400 dark:to-orange-400',
  },
};

export function DriveSidebar() {
  const navigate = useNavigate();
  const { storageUsed, plan, storageTotal, activeView, setActiveView, fetchUserStoragePlanAndConsumption } = useDriveStore();

  useEffect(() => {
    fetchUserStoragePlanAndConsumption();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const theme = PLAN_THEMES[plan] || PLAN_THEMES.DEFAULT;

  const storagePercentage = storageTotal > 0 ? Math.min(100, Math.max(0, (storageUsed / storageTotal) * 100)) : 0;
  const usedGBNum = storageUsed / (1024 * 1024 * 1024);
  const usedGB = Number(usedGBNum.toFixed(2));
  const totalGB = Math.round(storageTotal / (1024 * 1024 * 1024));

  const navigationItems = [
    { icon: HardDrive, label: 'My Drive', view: 'my-drive' },
    { icon: Clock, label: 'Recent', view: 'recent' },
    { icon: Star, label: 'Starred', view: 'starred' },
    { icon: Trash2, label: 'Trash', view: 'trash' },
  ];

  return (
    <aside className="w-64 h-screen sticky top-0 border-r bg-background p-4 flex flex-col overflow-hidden min-h-0 relative z-10">

      {/* scrollable area that can shrink */}
      <nav className="flex-1 overflow-y-auto space-y-1">
        {navigationItems.map((item) => (
          <Button
            key={item.label}
            variant={activeView === item.view ? 'secondary' : 'ghost'}
            onClick={() => setActiveView(item.view as any)}
            // Updated: Changed hardcoded hover:bg-gray-800 to adapt to light/dark
            className="w-full justify-start gap-3 h-10 text-foreground hover:bg-gray-100 dark:hover:bg-gray-800 transition-smooth"
          >
            <item.icon className="w-5 h-5" />
            {item.label}
          </Button>
        ))}
      </nav>

      <div
        className={cn(
          "shrink-0 p-4 rounded-xl shadow-lg mb-16 space-y-5",
          // Updated: Added light mode border and backdrop
          "backdrop-blur-md border border-gray-200 dark:border-white/10",
          theme.accent
        )}
      >
        {/* Header: Cloud icon + storage label */}
        <div className="flex items-center gap-3">
          <div
            className={cn(
              "flex items-center justify-center w-9 h-9 rounded-lg",
              // Updated: Light mode background
              "shadow-sm backdrop-blur-sm bg-gray-200/50 dark:bg-white/10"
            )}
          >
            <Cloud className={cn("w-4 h-4", theme.text)} />
          </div>

          <div>
            {/* Updated: Light mode text color */}
            <p className="text-xs text-gray-500 dark:text-white/60 font-medium uppercase tracking-wide">
              Storage
            </p>
            <p className={cn("text-sm font-semibold", theme.text)}>
              {usedGB} GB / {totalGB} GB
            </p>
          </div>
        </div>

        {/* Divider */}
        {/* Updated: Light mode border color */}
        <div className="border-t border-gray-200 dark:border-white/10" />

        {/* Progress bar section */}
        <div>
          {/* Updated: Light mode text color */}
          <div className="flex justify-between text-[11px] text-gray-500 dark:text-white/60 mb-1">
            <span>Used: {usedGB} GB</span>
            <span>{Math.round(storagePercentage)}%</span>
          </div>

          {/* Updated: Light mode track color */}
          <div className="w-full h-2 rounded-full bg-gray-200 dark:bg-white/10 overflow-hidden">
            <div
              className={cn(
                "h-full rounded-full transition-all duration-500 ease-out",
                theme.progress
              )}
              style={{ width: `${storagePercentage}%` }}
              aria-hidden
            />
          </div>
        </div>

        {/* Upgrade button */}
        <div className="flex justify-center">
          <Button
            onClick={() => navigate("/storagePlans")}
            className={cn(
              "w-full h-9 px-3 text-sm font-semibold rounded-lg shadow-md",
              "transition-transform transform hover:-translate-y-0.5",
              theme.button
            )}
          >
            Upgrade Storage
          </Button>
        </div>
      </div>

    </aside>
  );
}