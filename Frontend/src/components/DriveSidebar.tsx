import { 
  Plus, 
  HardDrive, 
  Users, 
  Clock, 
  Star, 
  Trash2, 
  Cloud,
  Smartphone,
  Monitor
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Progress } from '@/components/ui/progress';
import { useDriveStore } from '@/stores/driveStore';
import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom'; // ✅ import navigate

export function DriveSidebar() {
  const navigate = useNavigate(); // ✅ create navigate instance
  const { storageUsed, storageTotal, activeView, setActiveView, fetchUserStoragePlanAndConsumption } = useDriveStore();

  useEffect(() => {
    fetchUserStoragePlanAndConsumption();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

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
    <aside className="w-64 h-screen sticky top-0 border-r bg-background p-4 flex flex-col overflow-hidden min-h-0">
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

      {/* fixed footer; never scrolls out */}
      <div className="shrink-0 pt-6 border-t space-y-4 bg-background mb-16">
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <Cloud className="w-4 h-4" />
          <span>Storage</span>
        </div>

        <div className="space-y-2">
          <div className="flex justify-between text-sm">
            <span className="text-muted-foreground">
              {usedGB} GB of {totalGB} GB used
            </span>
          </div>
          <Progress value={storagePercentage} className="h-2" />
        </div>


        <Button 
        onClick={() => navigate('/storagePlans')}
        variant="outline" className="w-full text-sm h-9">
          Buy storage
        </Button>
      </div>
    </aside>
  );
}

