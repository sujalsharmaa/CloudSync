import { Search,X ,Grid3X3, List, Settings, HelpCircle, Filter, Moon, Sun, Monitor } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
  DropdownMenuLabel,
} from '@/components/ui/dropdown-menu';
import { useDriveStore } from '@/stores/driveStore';
import { useTheme } from '@/components/ThemeProvider';
import UserStatus from './UserStatus';

export function DriveHeader() {
  const { 
    searchQuery, 
    setSearchQuery, // This now triggers the backend search
    viewMode, 
    setViewMode, 
    user,
    fileTypeFilter,
    setFileTypeFilter,
    dateFilter,
    setDateFilter
  } = useDriveStore();
  const { theme, setTheme } = useTheme();

  return (
    <header className="h-16 border-b bg-background px-6 flex items-center justify-between relative z-10">
      <div className="flex items-center gap-6">
        <div className="flex items-center gap-3">
          <img
            className='w-20 h-20'
            src="../public/logo.png" alt="" />
          <button
            onClick={()=>{window.location.reload()}}
          >
            <h1 className="text-2xl font-medium text-foreground">CloudSync</h1>
          </button>
        </div>

        <div className="relative flex-1 max-w-4xl mx-auto">
          <div className="flex items-center gap-2">
            <div className="relative flex-1">
              {/* Search Icon */}
              <Search className="absolute left-8 top-1/2 transform -translate-y-1/2 w-5 h-5 text-muted-foreground" />

              {/* Search Input */}
              <Input
                placeholder="Search in Drive"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-12 pr-12 h-12 bg-muted/50 border-none focus:bg-background focus:ring-2 focus:ring-primary/20 transition-smooth text-base rounded-full w-[650px] ml-4"
              />

              {/* Clear (X) Button */}
              {searchQuery && (
                <button
                  onClick={() => setSearchQuery('')}
                  className="absolute right-6 top-1/2 transform -translate-y-1/2 text-gray-500 hover:text-gray-800 dark:text-gray-400 dark:hover:text-gray-200 transition"
                >
                  <X className="w-5 h-5" />
                </button>
              )}
            </div>
            
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="lg" className="h-12 w-12 p-0 rounded-full">
                  <Filter className="w-5 h-5" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-56">
                <DropdownMenuLabel>File Type</DropdownMenuLabel>
                <DropdownMenuItem onClick={() => setFileTypeFilter('all')}>
                  All files
                </DropdownMenuItem>
                <DropdownMenuItem onClick={() => setFileTypeFilter('folders')}>
                  Folders
                </DropdownMenuItem>
                <DropdownMenuItem onClick={() => setFileTypeFilter('documents')}>
                  Documents
                </DropdownMenuItem>
                <DropdownMenuItem onClick={() => setFileTypeFilter('images')}>
                  Images
                </DropdownMenuItem>
                <DropdownMenuItem onClick={() => setFileTypeFilter('videos')}>
                  Videos
                </DropdownMenuItem>
                <DropdownMenuItem onClick={() => setFileTypeFilter('presentations')}>
                  Presentations
                </DropdownMenuItem>
                <DropdownMenuItem onClick={() => setFileTypeFilter('spreadsheets')}>
                  Spreadsheets
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuLabel>Modified</DropdownMenuLabel>
                <DropdownMenuItem onClick={() => setDateFilter('all')}>
                  Any time
                </DropdownMenuItem>
                <DropdownMenuItem onClick={() => setDateFilter('today')}>
                  Today
                </DropdownMenuItem>
                <DropdownMenuItem onClick={() => setDateFilter('week')}>
                  Past week
                </DropdownMenuItem>
                <DropdownMenuItem onClick={() => setDateFilter('month')}>
                  Past month
                </DropdownMenuItem>
                <DropdownMenuItem onClick={() => setDateFilter('year')}>
                  Past year
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </div>
      </div>

      <div className="flex items-center gap-3">
        <div className="flex items-center gap-1 bg-muted/50 rounded-lg p-1">
          <Button
            variant={viewMode === 'grid' ? 'default' : 'ghost'}
            size="sm"
            onClick={() => setViewMode('grid')}
            className="h-8 w-8 p-0"
          >
            <Grid3X3 className="w-4 h-4" />
          </Button>
          <Button
            variant={viewMode === 'list' ? 'default' : 'ghost'}
            size="sm"
            onClick={() => setViewMode('list')}
            className="h-8 w-8 p-0"
          >
            <List className="w-4 h-4" />
          </Button>
        </div>

        <Button variant="ghost" size="sm" className="h-8 w-8 p-0">
          <HelpCircle className="w-4 h-4" />
        </Button>

        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="sm" className="h-8 w-8 p-0">
              {theme === 'dark' ? <Moon className="w-4 h-4" /> : theme === 'light' ? <Sun className="w-4 h-4" /> : <Monitor className="w-4 h-4" />}
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem onClick={() => setTheme('light')}>
              <Sun className="w-4 h-4 mr-2" />
              Light
            </DropdownMenuItem>
            <DropdownMenuItem onClick={() => setTheme('dark')}>
              <Moon className="w-4 h-4 mr-2" />
              Dark
            </DropdownMenuItem>
            <DropdownMenuItem onClick={() => setTheme('system')}>
              <Monitor className="w-4 h-4 mr-2" />
              System
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>

        <Button variant="ghost" size="sm" className="h-8 w-8 p-0">
          <Settings className="w-4 h-4" />
        </Button>
        <UserStatus/>
      </div>
    </header>
  );
}