import { DriveHeader } from '@/components/DriveHeader';
import { DriveSidebar } from '@/components/DriveSidebar';
import { FileGrid } from '@/components/FileGrid';
import { MetadataDiscoveryCard } from '@/components/MetaDataDiscoveryCard';
import { UploadZone } from '@/components/UploadZone';
import { useDriveStore } from '@/stores/driveStore';

const Index = () => {
  const { searchQuery, fileTypeFilter, dateFilter,setSearchQuery  } = useDriveStore();

  return (
    <div className="min-h-screen bg-background flex flex-col">
      <DriveHeader />
      
      <div className="flex flex-1">
        <DriveSidebar />
        
        <main className="flex-1 overflow-auto">
               <MetadataDiscoveryCard onSearch={setSearchQuery}/>
          <div className="p-6 space-y-6">
            <UploadZone />
            
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <h2 className="text-lg font-medium text-foreground">
                  {searchQuery ? `Search results for "${searchQuery}"` : 
                   fileTypeFilter !== 'all' ? `${fileTypeFilter.charAt(0).toUpperCase() + fileTypeFilter.slice(1)}` :
                   ''}
                </h2>
              </div>
              
              <FileGrid />
            </div>
          </div>
        </main>
      </div>
    </div>
  );
};

export default Index;