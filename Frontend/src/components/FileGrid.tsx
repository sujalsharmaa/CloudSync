import {
  Star,
  Share2,
  Download,
  Trash2,
  Loader2,
} from "lucide-react";
// Reverted to using '@/' alias based on error messages
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import SplashCursor from "@/components/SplashCursor"; // Assuming SplashCursor is here
import { useDriveStore, type DriveFile } from "@/stores/driveStore";
import { FilePreview } from "@/components/FilePreview";
import { cn } from "@/lib/utils";
import { useEffect, useState, useRef, useCallback } from "react";
import { useDebounce } from "@/useDebounce"; // Assuming useDebounce is in src/
import { FileActionsToolbar } from "@/components/FileActionsToolbar"; // Assuming FileActionsToolbar is here

// --- SPINNER COMPONENT ---
const Spinner = ({ className = "h-4 w-4 text-primary" }) => (
  <Loader2 className={cn("animate-spin", className)} />
);

// --- OVERLAY SPINNER COMPONENT ---
const ActionOverlaySpinner = () => (
  <div className="absolute inset-0 z-20 flex items-center justify-center bg-background/50 backdrop-blur-sm rounded-lg">
    <Loader2 className="h-8 w-8 animate-spin text-primary" /> {/* Larger spinner */}
  </div>
);

// --- SELECTION RECTANGLE COMPONENT ---
interface SelectionRectangleProps {
  start: { x: number; y: number };
  end: { x: number; y: number };
}
const SelectionRectangle = ({ start, end }: SelectionRectangleProps) => {
  const left = Math.min(start.x, end.x);
  const top = Math.min(start.y, end.y);
  const width = Math.abs(end.x - start.x);
  const height = Math.abs(end.y - start.y);

  if (width < 5 || height < 5) return null;

  return (
    <div
      className="absolute z-30 border-2 border-primary bg-primary/20 pointer-events-none"
      style={{
        left: `${left}px`,
        top: `${top}px`,
        width: `${width}px`,
        height: `${height}px`,
      }}
    />
  );
};
// -------------------------------------------------------------------------

/** Safe formatting helpers */
const formatFileSize = (bytes?: number) => {
  // ... (helper function remains the same)
  if (!bytes && bytes !== 0) return "";
  const sizes = ["B", "KB", "MB", "GB"];
  if (bytes === 0) return "0 B";
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  return Math.round((bytes / Math.pow(1024, i)) * 100) / 100 + " " + sizes[i];
};

const formatDate = (dateString?: string) => {
  // ... (helper function remains the same)
  if (!dateString) return "";
  const date = new Date(dateString);
  if (isNaN(date.getTime())) return "";

  const now = new Date();
  const diffTime = now.getTime() - date.getTime();
  const oneMinute = 1000 * 60;
  const oneHour = 1000 * 60 * 60;
  const oneDay = 1000 * 60 * 60 * 24;

  const startOfDay = (d: Date) =>
    new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
  const dateDay = startOfDay(date);
  const nowDay = startOfDay(now);

  if (dateDay === nowDay) {
    if (diffTime < oneMinute) return "Just now";
    if (diffTime < oneHour) {
      const diffMinutes = Math.floor(diffTime / oneMinute);
      return `${diffMinutes} mins ago`;
    }
    const diffHours = Math.floor(diffTime / oneHour);
    return `${diffHours} hours ago`;
  }
  if (nowDay - dateDay === oneDay) return "Yesterday";

  const diffDays = Math.floor(diffTime / oneDay);
  if (diffDays < 7) return `${diffDays} days ago`;
  if (diffDays < 30) return `${Math.ceil(diffDays / 7)} weeks ago`;
  return date.toLocaleDateString();
};

export function FileGrid() {
  const {
    selectedFiles,
    viewMode,
    toggleFileSelection,
    toggleStar,
    files = [],
    searchQuery = "",
    fileTypeFilter = "all",
    dateFilter = "all",
    fetchFiles,
    MoveFileToTrash,
    activeView,
    fetchStarredFiles,
    fetchRecycledFiles,
    addFile,
    isLoading,
    //isUploading,
    fileActionLoading: fileActionLoadingSet,
    setSelectedFiles, // Make sure this exists in your store, or add it
  } = useDriveStore();

  // Drag selection state
  const [isSelecting, setIsSelecting] = useState(false);
  const [selectionStart, setSelectionStart] = useState<{ x: number; y: number } | null>(null);
  const [selectionEnd, setSelectionEnd] = useState<{ x: number; y: number } | null>(null);
  
  const containerRef = useRef<HTMLDivElement>(null);
  const fileRefs = useRef<Map<string, HTMLDivElement>>(new Map());

  const gradients = [
    "from-indigo-500 via-sky-500 to-teal-500",
    "from-pink-500 via-red-500 to-yellow-500",
    "from-purple-500 via-fuchsia-500 to-pink-500",
    "from-green-400 via-emerald-500 to-teal-400",
    "from-orange-400 via-amber-500 to-yellow-400",
    "from-blue-400 via-cyan-400 to-emerald-400",
    "from-violet-500 via-indigo-500 to-blue-500",
    "from-rose-500 via-pink-400 to-purple-400",
    "from-lime-400 via-green-400 to-emerald-500",
    "from-sky-400 via-indigo-400 to-violet-500",
  ];

  const [index, setIndex] = useState(0);

  useEffect(() => {
    const interval = setInterval(() => {
      setIndex((prev) => (prev + 1) % gradients.length);
    }, 30000);
    return () => clearInterval(interval);
  }, []);

  const handleToggleStar = async (fileId: string) => {
    await toggleStar(fileId);
  };

  const debouncedSearchQuery = useDebounce(searchQuery, 500);

  useEffect(() => {
    console.log("Fetching files for view:", activeView, "Query:", debouncedSearchQuery);
    if (activeView === "trash") {
      fetchRecycledFiles(debouncedSearchQuery);
    } else if (activeView === "starred") {
      fetchStarredFiles(/* debouncedSearchQuery */);
    } else {
      fetchFiles(debouncedSearchQuery);
    }
  }, [
    activeView,
    debouncedSearchQuery,
    fetchFiles,
    fetchRecycledFiles,
    fetchStarredFiles,
  ]);

  // Drag selection handlers
  const handleMouseDown = useCallback((e: React.MouseEvent) => {
    // Only start selection with left mouse button and not on interactive elements
    if (e.button !== 0 || 
        (e.target as Element).closest('button') || 
        (e.target as Element).closest('input') ||
        (e.target as Element).closest('.file-actions-toolbar')) {
      return;
    }

    setIsSelecting(true);
    setSelectionStart({ x: e.clientX, y: e.clientY });
    setSelectionEnd({ x: e.clientX, y: e.clientY });

    // Clear selection if not holding Ctrl/Cmd
    if (!e.ctrlKey && !e.metaKey) {
      setSelectedFiles([]);
    }
  }, [setSelectedFiles]);

  const handleMouseMove = useCallback((e: MouseEvent) => {
    if (!isSelecting || !selectionStart) return;

    setSelectionEnd({ x: e.clientX, y: e.clientY });

    // Calculate selected files
    if (containerRef.current) {
      const containerRect = containerRef.current.getBoundingClientRect();
      const selectionRect = {
        left: Math.min(selectionStart.x, e.clientX) - containerRect.left,
        top: Math.min(selectionStart.y, e.clientY) - containerRect.top,
        right: Math.max(selectionStart.x, e.clientX) - containerRect.left,
        bottom: Math.max(selectionStart.y, e.clientY) - containerRect.top,
      };

      const newlySelectedFiles: string[] = [];

      fileRefs.current.forEach((fileElement, fileId) => {
        const fileRect = fileElement.getBoundingClientRect();
        const relativeFileRect = {
          left: fileRect.left - containerRect.left,
          top: fileRect.top - containerRect.top,
          right: fileRect.right - containerRect.left,
          bottom: fileRect.bottom - containerRect.top,
        };

        // Check if file element intersects with selection rectangle
        const intersects = !(
          selectionRect.right < relativeFileRect.left ||
          selectionRect.left > relativeFileRect.right ||
          selectionRect.bottom < relativeFileRect.top ||
          selectionRect.top > relativeFileRect.bottom
        );

        if (intersects) {
          newlySelectedFiles.push(fileId);
        }
      });

      // Update selection (add to existing if Ctrl/Cmd is held)
      if (newlySelectedFiles.length > 0) {
        const currentSelection = new Set(selectedFiles);
        newlySelectedFiles.forEach(fileId => currentSelection.add(fileId));
        setSelectedFiles(Array.from(currentSelection));
      }
    }
  }, [isSelecting, selectionStart, selectedFiles, setSelectedFiles]);

  const handleMouseUp = useCallback(() => {
    setIsSelecting(false);
    setSelectionStart(null);
    setSelectionEnd(null);
  }, []);

  // Add event listeners for drag selection
  useEffect(() => {
    if (isSelecting) {
      document.addEventListener('mousemove', handleMouseMove);
      document.addEventListener('mouseup', handleMouseUp);
      
      return () => {
        document.removeEventListener('mousemove', handleMouseMove);
        document.removeEventListener('mouseup', handleMouseUp);
      };
    }
  }, [isSelecting, handleMouseMove, handleMouseUp]);

  const validFiles = (files ?? []).filter(file => file && file.id);

  const filteredFiles = validFiles.filter((file: DriveFile) => {
     if (fileTypeFilter !== 'all') {
         let typeMatch = false;
         const fileExt = file.fileName?.split('.').pop()?.toLowerCase();
         switch (fileTypeFilter) {
             case 'documents':
                 typeMatch = ['doc', 'docx', 'pdf', 'txt', 'rtf', 'odt'].includes(fileExt || '');
                 break;
             case 'images':
                 typeMatch = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'svg', 'webp', 'tiff'].includes(fileExt || '');
                 break;
             case 'videos':
                 typeMatch = ['mp4', 'mov', 'avi', 'mkv', 'wmv', 'flv', 'webm'].includes(fileExt || '');
                 break;
             case 'presentations':
                 typeMatch = ['ppt', 'pptx', 'odp'].includes(fileExt || '');
                 break;
             case 'spreadsheets':
                 typeMatch = ['xls', 'xlsx', 'csv', 'ods'].includes(fileExt || '');
                 break;
         }
         if (!typeMatch) return false;
     }

     if (dateFilter !== 'all') {
         const fileDate = file.processedAt ? new Date(file.processedAt) : null;
         if (!fileDate) return false;

         const now = new Date();
         const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate());

         let startDate: Date | null = null;

         switch (dateFilter) {
             case 'today':
                 startDate = todayStart;
                 break;
             case 'week':
                 startDate = new Date(todayStart);
                 startDate.setDate(todayStart.getDate() - 7);
                 break;
             case 'month':
                 startDate = new Date(todayStart);
                 startDate.setMonth(todayStart.getMonth() - 1);
                 break;
             case 'year':
                 startDate = new Date(todayStart);
                 startDate.setFullYear(todayStart.getFullYear() - 1);
                 break;
         }
         if (startDate && fileDate < startDate) {
             return false;
         }
     }
    return true;
  });

  const sortedFiles = filteredFiles.slice().sort((a, b) => {
    const timeA = a.processedAt ? new Date(a.processedAt).getTime() : 0;
    const timeB = b.processedAt ? new Date(b.processedAt).getTime() : 0;
    return (isNaN(timeB) ? 0 : timeB) - (isNaN(timeA) ? 0 : timeA);
  });

  // Register file element refs
  const registerFileRef = useCallback((fileId: string, element: HTMLDivElement | null) => {
    if (element) {
      fileRefs.current.set(fileId, element);
    } else {
      fileRefs.current.delete(fileId);
    }
  }, []);

  return (
    <div className="space-y-4">
      <h1
        className={`text-center text-5xl md:text-6xl font-extrabold tracking-tight
           bg-gradient-to-r ${gradients[index]}
           bg-clip-text text-transparent drop-shadow-md
           transition-all duration-1000 ease-in-out select-none`}
      >
        {activeView === 'my-drive' ? 'My Drive' :
         activeView === 'shared-with-me' ? 'Shared With Me' :
         activeView.charAt(0).toUpperCase() + activeView.slice(1).replace("-", " ")}
      </h1>

       {isLoading && sortedFiles.length === 0 && (
         <div className="flex justify-center items-center h-64">
           <Loader2 className="h-12 w-12 animate-spin text-primary" />
         </div>
       )}

      {!isLoading && sortedFiles.length === 0 && (
         <div className="text-center py-16 text-muted-foreground">
              <p className="text-lg">No files found.</p>
              {activeView === 'my-drive' && <p>Upload some files to get started!</p>}
         </div>
       )}

      {sortedFiles.length > 0 && (
        viewMode === "list" ? (
        <div 
          ref={containerRef}
          className="space-y-1 relative"
          onMouseDown={handleMouseDown}
        >
          {/* Selection Rectangle */}
          {isSelecting && selectionStart && selectionEnd && (
            <SelectionRectangle start={selectionStart} end={selectionEnd} />
          )}

          <div className="grid grid-cols-[auto_1fr_120px_120px_auto_40px] gap-4 px-4 py-2 text-sm text-muted-foreground border-b">
            <div className="w-6"></div>
            <div>Name</div>
            <div>Owner</div>
            <div>Last modified</div>
            <div>File size</div>
            <div></div>
          </div>

          {sortedFiles.map((file) => {
            if (!file || !file.id) return null;

            const isSelected = selectedFiles.includes(file.id);
            const isFileActionLoading = fileActionLoadingSet.has(file.id);

            return (
              <div
                key={file.id}
                ref={(el) => registerFileRef(file.id, el)}
                className={cn(
                  "grid grid-cols-[auto_1fr_120px_120px_auto_40px] items-center gap-4 px-4 py-3 hover:bg-muted/50 rounded-lg transition-smooth group cursor-pointer relative file-item",
                  isSelected && "bg-muted/50",
                  isLoading && "opacity-50 pointer-events-none transition-opacity"
                )}
                onClick={isFileActionLoading ? (e) => e.stopPropagation() : () => toggleFileSelection(file.id)}
              >
                {isFileActionLoading && <ActionOverlaySpinner />}

                <Checkbox
                  checked={isSelected}
                  onCheckedChange={isFileActionLoading ? undefined : () => toggleFileSelection(file.id)}
                  className="mt-0 opacity-100 group-hover:opacity-100 transition-smooth disabled:opacity-50"
                  disabled={isFileActionLoading}
                  aria-label={`Select file ${file.fileName ?? "Unnamed"}`}
                />

                <div className="flex items-center gap-3 min-w-0">
                  <FilePreview file={file} size="sm" />
                  <span className="truncate text-foreground">
                    {file.fileName ?? "Unnamed"}
                  </span>
                  {file.starred && (
                    <Star className="w-4 h-4 text-drive-yellow fill-current flex-shrink-0" />
                  )}
                  {file.shared && (
                    <Share2 className="w-4 h-4 text-muted-foreground flex-shrink-0" />
                  )}
                </div>

                <div className="text-sm text-muted-foreground truncate">
                  {file.owner || "You"}
                </div>

                <div className="text-sm text-muted-foreground">
                  {formatDate(file.processedAt)}
                </div>

                <div className="text-sm text-muted-foreground">
                  {formatFileSize(file.size)}
                </div>

                <div className="flex justify-end items-center">
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-6 w-6 p-0 opacity-0 group-hover:opacity-100 transition-smooth"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleToggleStar(file.id);
                    }}
                    disabled={isFileActionLoading}
                    aria-label={file.starred ? "Unstar file" : "Star file"}
                  >
                    <Star
                      className={cn(
                        "w-4 h-4",
                        file.starred
                          ? "text-drive-yellow fill-current"
                          : "text-muted-foreground"
                      )}
                    />
                  </Button>
                </div>
              </div>
            );
          })}
          <FileActionsToolbar />
        </div>
      ) : (
        <div 
          ref={containerRef}
          className="relative"
          onMouseDown={handleMouseDown}
        >
          {/* Selection Rectangle */}
          {isSelecting && selectionStart && selectionEnd && (
            <SelectionRectangle start={selectionStart} end={selectionEnd} />
          )}

          {/* <div className="absolute inset-0 z-0 overflow-hidden rounded-lg pointer-events-none">
             <SplashCursor />
          </div> */}

          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4 p-4 relative z-10">
            {sortedFiles.map((file) => {
               if (!file || !file.id) return null;

              const isSelected = selectedFiles.includes(file.id);
              const isFileActionLoading = fileActionLoadingSet.has(file.id);

              return (
                <Card
                  key={file.id}
                  ref={(el) => registerFileRef(file.id, el)}
                  className={cn(
                    "p-4 hover:shadow-hover transition-smooth cursor-pointer group animate-scale-in relative z-10 overflow-hidden file-item",
                    isSelected && "ring-2 ring-primary",
                    isLoading && "opacity-50 pointer-events-none transition-opacity"
                  )}
                  onClick={isFileActionLoading ? (e) => e.stopPropagation() : () => toggleFileSelection(file.id)}
                >
                  {isFileActionLoading && <ActionOverlaySpinner />}

                  <div className="flex items-start justify-between mb-3">
                    <FilePreview file={file} size="lg" />
                    <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-smooth">
                      <Button
                        variant="ghost"
                        size="sm"
                        className="h-6 w-6 p-0"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleToggleStar(file.id);
                        }}
                        disabled={isFileActionLoading}
                        aria-label={file.starred ? "Unstar file" : "Star file"}
                      >
                        <Star
                          className={cn(
                            "w-4 h-4",
                            file.starred
                              ? "text-drive-yellow fill-current"
                              : "text-muted-foreground"
                          )}
                        />
                      </Button>
                    </div>
                  </div>

                  <div className="space-y-1">
                    <h3 className="font-medium text-sm text-foreground truncate">
                      {file.fileName ?? "Unnamed"}
                    </h3>
                    <div className="flex items-center gap-2 text-xs text-muted-foreground">
                      <span>{formatDate(file.processedAt)}</span>
                      {file.size != null && (
                        <>
                          <span>•</span>
                          <span>{formatFileSize(file.size)}</span>
                        </>
                      )}
                    </div>
                  </div>
                </Card>
              );
            })}
            <FileActionsToolbar />
          </div>
        </div>
      ))}
    </div>
  );
}