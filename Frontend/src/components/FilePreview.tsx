import { 
  FileText, 
  FolderOpen, 
  Image as ImageIcon, 
  FileSpreadsheet, 
  Presentation,
  FileVideo,
  FileAudio,
  File
} from 'lucide-react';
import { DriveFile } from '@/stores/driveStore';
import { cn } from '@/lib/utils';

interface FilePreviewProps {
  file: DriveFile;
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}

const getFileType = (file: DriveFile) => {
  const fileType = file.fileType?.toLowerCase();

  if (!fileType) return 'file';
  if (fileType === 'folder') return 'folder';
  if (fileType.includes('image')) return 'image';
  if (fileType.includes('video')) return 'video';
  if (fileType.includes('audio')) return 'audio';
  if (fileType.includes('text')) return 'text';
  if (fileType.includes('pdf')) return 'pdf';
  if (fileType.includes('spreadsheet') || fileType.includes('excel')) return 'spreadsheet';
  if (fileType.includes('presentation') || fileType.includes('powerpoint')) return 'presentation';
  return 'file';
};

const getFileIcon = (file: DriveFile) => {
  const fileType = getFileType(file);
  
  switch (fileType) {
    case 'folder': return FolderOpen;
    case 'image': return ImageIcon;
    case 'video': return FileVideo;
    case 'audio': return FileAudio;
    case 'text': return FileText;
    case 'pdf': return FileText;
    case 'spreadsheet': return FileSpreadsheet;
    case 'presentation': return Presentation;
    default: return File;
  }
};

const getFileIconColor = (file: DriveFile) => {
  const fileType = getFileType(file);
  
  switch (fileType) {
    case 'folder': return 'text-drive-blue';
    case 'image': return 'text-drive-purple';
    case 'video': return 'text-drive-red';
    case 'audio': return 'text-drive-yellow';
    case 'text': return 'text-drive-blue';
    case 'pdf': return 'text-drive-red';
    case 'spreadsheet': return 'text-drive-green';
    case 'presentation': return 'text-drive-orange';
    default: return 'text-muted-foreground';
  }
};

// Reusable thumbnail placeholder wrapper for consistent styling
const ThumbnailPlaceholder = ({ children }: { children: React.ReactNode }) => (
  <div className="relative w-full h-full rounded-lg bg-muted/20 flex items-center justify-center overflow-hidden">
    {children}
  </div>
);

// The core logic change is here
const generateThumbnail = (file: DriveFile) => {
  // Use a real thumbnail if the backend provides a URL
  if (file.thumbnailUrl) {
    const fileType = getFileType(file);
    switch (fileType) {
      case 'image':
        return <img src={file.thumbnailUrl} alt={file.fileName} className="object-cover w-full h-full rounded-lg" />;
      case 'video':
        // For video, you would use a <video> tag with a poster attribute
        return <video src={file.thumbnailUrl} poster={file.thumbnailUrl} className="object-cover w-full h-full rounded-lg" muted playsInline />;
      case 'pdf':
      case 'text':
      case 'spreadsheet':
      case 'presentation':
        // For documents, the thumbnail is an image representation of the first page
        return <img src={file.thumbnailUrl} alt={file.fileName} className="object-cover w-full h-full rounded-lg" />;
      default:
        // Fallback to the generic icon placeholder
        return (
          <ThumbnailPlaceholder>
            <File className="w-12 h-12 text-muted-foreground/50" />
          </ThumbnailPlaceholder>
        );
    }
  }

  // Fallback to the simplified static placeholders if no thumbnail URL is provided
  if (file.fileType?.includes('image')) {
    return (
      <ThumbnailPlaceholder>
        <ImageIcon className="w-12 h-12 text-muted-foreground/50" />
      </ThumbnailPlaceholder>
    );
  }
  
  if (file.fileType?.includes('text') || file.fileType?.includes('pdf')) {
    return (
      <div className="w-full h-full rounded-lg bg-white dark:bg-gray-800 border border-border p-2 shadow-sm">
        <div className="space-y-1">
          <div className="h-1 bg-foreground/20 rounded w-3/4"></div>
          <div className="h-1 bg-foreground/15 rounded w-full"></div>
          <div className="h-1 bg-foreground/10 rounded w-2/3"></div>
          <div className="h-1 bg-foreground/15 rounded w-4/5"></div>
          <div className="h-1 bg-foreground/10 rounded w-1/2"></div>
        </div>
      </div>
    );
  }

  if (file.fileType?.includes('spreadsheet')) {
    return (
      <ThumbnailPlaceholder>
        <div className="grid grid-cols-3 grid-rows-3 w-3/4 h-3/4 gap-1 p-2 bg-white dark:bg-gray-800 border border-border">
          {Array.from({ length: 9 }).map((_, i) => (
            <div key={i} className="bg-foreground/10 rounded-sm"></div>
          ))}
        </div>
      </ThumbnailPlaceholder>
    );
  }

  if (file.fileType?.includes('presentation')) {
    return (
      <ThumbnailPlaceholder>
        <div className="w-3/4 h-1/2 bg-white dark:bg-gray-800 border border-border p-1 shadow-sm flex flex-col items-center justify-center gap-1">
          <div className="h-2 bg-foreground/20 rounded w-full"></div>
          <div className="h-1 bg-foreground/15 rounded w-1/2"></div>
        </div>
      </ThumbnailPlaceholder>
    );
  }
  
  return null;
};

export function FilePreview({ file, size = 'md', className }: FilePreviewProps) {
  const Icon = getFileIcon(file);
  const iconColor = getFileIconColor(file);
  const thumbnail = generateThumbnail(file);
  
  const sizeClasses = {
    sm: 'w-8 h-8',
    md: 'w-12 h-12',
    lg: 'w-16 h-16'
  };
  
  const iconSizes = {
    sm: 'w-4 h-4',
    md: 'w-6 h-6',
    lg: 'w-8 h-8'
  };

  return (
    <div className={cn('relative', sizeClasses[size], className)}>
      {thumbnail ? (
        <div className="w-full h-full">
          {thumbnail}
          <div className="absolute -bottom-1 -right-1 bg-background rounded-full p-0.5 border border-border shadow-sm">
            <Icon className={cn(iconSizes.sm, iconColor)} />
          </div>
        </div>
      ) : (
        <div className="w-full h-full flex items-center justify-center">
          <Icon className={cn(iconSizes[size], iconColor)} />
        </div>
      )}
    </div>
  );
}