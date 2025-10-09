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
import { useNavigate } from 'react-router-dom'; // ✅ import navigate
import { cn } from '@/lib/utils'; // Utility for conditional class names

// Themes are now specifically for the storage block appearance
const PLAN_THEMES = {
  DEFAULT: {
    accent: 'bg-white/5 border-white/10', // Neutral dark background for storage block
    text: 'text-white/80',
    button: 'bg-gradient-to-r from-white-600 to-gray-700 text-black',
    progress: 'bg-gradient-to-r from-gray-400 to-gray-500',
  },
  BASIC: {
    accent: 'bg-gradient-to-r from-blue-700/20 to-blue-900/20 border-blue-500/30',
    text: 'text-blue-200',
    button: 'bg-gradient-to-r from-blue-600 to-blue-700 text-white',
    progress: 'bg-gradient-to-r from-blue-400 to-blue-600',
  },
  PRO: {
    accent: 'bg-gradient-to-r from-purple-700/20 to-pink-900/20 border-purple-500/30',
    text: 'text-purple-200',
    button: 'bg-gradient-to-r from-purple-600 to-pink-600 text-white',
    progress: 'bg-gradient-to-r from-indigo-400 to-pink-500',
  },
  TEAM: {
    accent: 'bg-gradient-to-r from-rose-700/20 to-orange-900/20 border-rose-500/30',
    text: 'text-rose-200',
    button: 'bg-gradient-to-r from-rose-600 to-orange-600 text-white',
    progress: 'bg-gradient-to-r from-rose-400 to-orange-400',
  },
};

export function DriveSidebar() {
  const navigate = useNavigate(); // ✅ create navigate instance
  const { storageUsed,plan, storageTotal, activeView, setActiveView, fetchUserStoragePlanAndConsumption } = useDriveStore();

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
    { icon: Users, label: 'Shared with me', view: 'shared-with-me' },
    { icon: Clock, label: 'Recent', view: 'recent' },
    { icon: Star, label: 'Starred', view: 'starred' },
    { icon: Trash2, label: 'Trash', view: 'trash' },
  ];

  return (
    <aside className="w-64 h-screen sticky top-0 border-r bg-background p-4 flex flex-col overflow-hidden min-h-0 relative z-10">
      <Button className="mb-6 w-full justify-start gap-3 h-12 bg-gray-800 hover:bg-gray-600 shadow-hover transition-smooth text-white">
        <Plus className="w-5 h-5" />
        New
      </Button>

      {/* scrollable area that can shrink */}
      <nav className="flex-1 overflow-y-auto space-y-1">
        {navigationItems.map((item) => (
          <Button
            key={item.label}
            variant={activeView === item.view ? 'secondary' : 'ghost'}
            onClick={() => setActiveView(item.view as any)}
            className="w-full justify-start gap-3 h-10 text-foreground hover:bg-gray-800 transition-smooth"
          >
            <item.icon className="w-5 h-5" />
            {item.label}
          </Button>
        ))}
      </nav>

     <div
  className={cn(
    "shrink-0 p-4 rounded-xl shadow-lg mb-16 space-y-5",
    "backdrop-blur-md border border-white/10",
    theme.accent
  )}
>
  {/* Header: Cloud icon + storage label */}
  <div className="flex items-center gap-3">
    <div
      className={cn(
        "flex items-center justify-center w-9 h-9 rounded-lg bg-white/10",
        "shadow-sm backdrop-blur-sm",
        theme.glow
      )}
    >
      <Cloud className={cn("w-4 h-4", theme.text)} />
    </div>

    <div>
      <p className="text-xs text-white/60 font-medium uppercase tracking-wide">
        Storage
      </p>
      <p className={cn("text-sm font-semibold", theme.text)}>
        {usedGB} GB / {totalGB} GB
      </p>
    </div>
  </div>

  {/* Divider */}
  <div className="border-t border-white/10" />

  {/* Progress bar section */}
  <div>
    <div className="flex justify-between text-[11px] text-white/60 mb-1">
      <span>Used: {usedGB} GB</span>
      <span>{Math.round(storagePercentage)}%</span>
    </div>

    <div className="w-full h-2 rounded-full bg-white/10 overflow-hidden">
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

  {/* Upgrade button — moved below progress */}
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

  {/* Metadata row */}
  <div className="flex items-center justify-between text-xs text-white/60 pt-1">

  </div>
</div>

    </aside>
  );
}

