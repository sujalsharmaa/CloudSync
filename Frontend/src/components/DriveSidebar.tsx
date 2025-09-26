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

export function DriveSidebar() {
  const { storageUsed, storageTotal,activeView, setActiveView } = useDriveStore();
  
  const storagePercentage = (storageUsed / storageTotal) * 100;
  const usedGB = Math.round(storageUsed / (1024 * 1024 * 1024));
  const totalGB = Math.round(storageTotal / (1024 * 1024 * 1024));

  const navigationItems = [
    { icon: HardDrive, label: 'My Drive', view: 'my-drive' },
    { icon: Users, label: 'Shared with me', view: 'shared-with-me' },
    { icon: Clock, label: 'Recent', view: 'recent' },
    { icon: Star, label: 'Starred', view: 'starred' },
    { icon: Trash2, label: 'Trash', view: 'trash' },
  ];

  return (
    <aside className="w-64 border-r bg-background p-4 flex flex-col">
      <Button className="mb-6 w-full justify-start gap-3 h-12 bg-gray-800 hover:bg-gray-600 shadow-hover transition-smooth text-white">
        <Plus className="w-5 h-5" />
        New
      </Button>

      <nav className="flex-1 space-y-1">
        {navigationItems.map((item) => (
          <Button
            key={item.label}
            variant={activeView === item.view ? "secondary" : "ghost"} // Use activeView for styling
            onClick={() => setActiveView(item.view as any)} // Call setActiveView
            className="w-full justify-start gap-3 h-10 text-foreground hover:bg-gray-800 transition-smooth"
          >
            <item.icon className="w-5 h-5" />
            {item.label}
          </Button>
        ))}
      </nav>

      <div className="mt-8 space-y-4">
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <Cloud className="w-4 h-4" />
          <span>Storage</span>
        </div>
        
        <div className="space-y-2">
          <div className="flex justify-between text-sm">
            <span className="text-muted-foreground">{usedGB} GB of {totalGB} GB used</span>
          </div>
          <Progress value={storagePercentage} className="h-2" />
        </div>

        <div className="space-y-2">
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <div className="w-2 h-2 bg-drive-blue rounded-full"></div>
            <span>Drive</span>
            <span className="ml-auto">{Math.round(usedGB * 0.6)} GB</span>
          </div>
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <div className="w-2 h-2 bg-drive-red rounded-full"></div>
            <span>Gmail</span>
            <span className="ml-auto">{Math.round(usedGB * 0.3)} GB</span>
          </div>
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <div className="w-2 h-2 bg-drive-green rounded-full"></div>
            <span>Photos</span>
            <span className="ml-auto">{Math.round(usedGB * 0.1)} GB</span>
          </div>
        </div>

        <Button variant="outline" className="w-full text-sm h-9">
          Buy storage
        </Button>

        <div className="pt-4 border-t space-y-1">
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <Monitor className="w-3 h-3" />
            <span>Computers</span>
          </div>
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <Smartphone className="w-3 h-3" />
            <span>Mobile devices</span>
          </div>
        </div>
      </div>
    </aside>
  );
}