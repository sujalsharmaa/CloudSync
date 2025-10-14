import {
  MoreVertical,
  Star,
  Share2,
  Download,
  Trash2,
  Loader2, // Import Loader2 for the spinner icon
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import SplashCursor from "./SplashCursor"; // Assuming the path is correct
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useDriveStore, type DriveFile } from "@/stores/driveStore";
import { FilePreview } from "@/components/FilePreview";
import { cn } from "@/lib/utils";
import { useEffect, useState } from "react";
import { useDebounce } from "@/useDebounce";
import { FileActionsToolbar } from "./FileActionsToolbar";

// --- SPINNER COMPONENT (Replace with your shared component if available) ---
const Spinner = ({ className = "h-4 w-4 text-primary" }) => (
  <Loader2 className={cn("animate-spin", className)} />
);
// -------------------------------------------------------------------------


/** Safe formatting helpers */
const formatFileSize = (bytes?: number) => {
  if (!bytes && bytes !== 0) return "";
  const sizes = ["B", "KB", "MB", "GB"];
  if (bytes === 0) return "0 B";
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  return Math.round((bytes / Math.pow(1024, i)) * 100) / 100 + " " + sizes[i];
};

const formatDate = (dateString?: string) => {
  if (!dateString) return "";
  const date = new Date(dateString);
  if (isNaN(date.getTime())) return "";

  const now = new Date();
  const diffTime = now.getTime() - date.getTime(); // Difference in milliseconds
  const oneMinute = 1000 * 60;
  const oneHour = 1000 * 60 * 60;
  const oneDay = 1000 * 60 * 60 * 24;

  // --- 1. Check for "Just now" and "X mins/hours ago" (Same calendar day) ---

  // Normalize both dates to the start of their respective days (local timezone)
  const startOfDay = (d: Date) =>
    new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();

  const dateDay = startOfDay(date);
  const nowDay = startOfDay(now);

  if (dateDay === nowDay) {
    // If the file's day is today (even if the time zone shift makes it appear negative/small)
    if (diffTime < oneMinute) {
      return "Just now";
    }
    if (diffTime < oneHour) {
      const diffMinutes = Math.floor(diffTime / oneMinute);
      return `${diffMinutes} mins ago`;
    }
    const diffHours = Math.floor(diffTime / oneHour);
    return `${diffHours} hours ago`;
  }

  // --- 2. Check for "Yesterday" ---

  if (nowDay - dateDay === oneDay) {
    return "Yesterday";
  }

  // --- 3. Check for "X days/weeks ago" ---

  // Use Math.ceil(diffTime / oneDay) only for past calendar days,
  // but we already handled "today" and "yesterday" accurately.
  const diffDays = Math.floor(diffTime / oneDay);

  if (diffDays < 7) return `${diffDays} days ago`;
  if (diffDays < 30) return `${Math.ceil(diffDays / 7)} weeks ago`;

  // Fallback to localized date format
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
    activeView, // Add activeView here
    fetchStarredFiles,
    fetchRecycledFiles,
    addFile,
    isLoading, // <-- Get global loading state
    //connectWebSocket,
  } = useDriveStore();
  
  // Local state to manage individual file action loading (e.g., star or trash)
  const [fileActionLoading, setFileActionLoading] = useState<string | null>(null);

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
    }, 30000); // change every 30 seconds
    return () => clearInterval(interval);
  }, []);

  const handleMoveToTrash = async (fileToTrash: DriveFile) => {
    setFileActionLoading(fileToTrash.id);
    try {
      // Call the store action with the single file in an array
      await MoveFileToTrash([fileToTrash]);
    } finally {
      setFileActionLoading(null);
    }
  };
  
  const handleToggleStar = async (fileId: string) => {
    setFileActionLoading(fileId);
    try {
      await toggleStar(fileId);
    } finally {
      setFileActionLoading(null);
    }
  };

  const debouncedSearchQuery = useDebounce(searchQuery, 500);

  useEffect(() => {
    if (activeView === "trash") {
      fetchRecycledFiles(debouncedSearchQuery);
    } else if (activeView === "starred") {
      fetchStarredFiles();
    } else {
      fetchFiles(debouncedSearchQuery);
    }
  }, [
    activeView,
    debouncedSearchQuery,
    fetchFiles,
    fetchRecycledFiles,
    fetchStarredFiles,
    addFile,
  ]);

  const filteredFiles = (files ?? []).filter((file: DriveFile) => {
    let matchesType = true;
    if (fileTypeFilter !== "all") {
      switch (fileTypeFilter) {
        case "folders":
          matchesType = file.fileType === "folder";
          break;
        case "documents":
          matchesType =
            (file.fileType?.includes("document") ?? false) ||
            (file.fileType?.includes("text") ?? false);
          break;
        case "images":
          matchesType = file.fileType?.includes("image") ?? false;
          break;
        case "videos":
          matchesType = file.fileType?.includes("video") ?? false;
          break;
        case "presentations":
          matchesType = file.fileType?.includes("presentation") ?? false;
          break;
        case "spreadsheets":
          matchesType = file.fileType?.includes("spreadsheet") ?? false;
          break;
        default:
          matchesType = true;
      }
    }

    let matchesDate = true;
    if (dateFilter !== "all") {
      const processedAt = file.processedAt ? new Date(file.processedAt) : null;
      if (!processedAt || isNaN(processedAt.getTime())) {
        matchesDate = false;
      } else {
        const now = new Date();
        const diffTime = now.getTime() - processedAt.getTime();
        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

        switch (dateFilter) {
          case "today":
            matchesDate = diffDays <= 1;
            break;
          case "week":
            matchesDate = diffDays <= 7;
            break;
          case "month":
            matchesDate = diffDays <= 30;
            break;
          case "year":
            matchesDate = diffDays <= 365;
            break;
          default:
            matchesDate = true;
        }
      }
    }

    return matchesType && matchesDate;
  });

  const sortedFiles = filteredFiles.slice().sort((a, b) => {
    const dateA = a.processedAt ? new Date(a.processedAt).getTime() : 0;
    const dateB = b.processedAt ? new Date(b.processedAt).getTime() : 0;

    // Sort descending (b - a) so the newer file (larger timestamp) comes first
    return dateB - dateA;
  });

  return (
    <div className="space-y-4">
      <h1
        className={`text-center text-5xl md:text-6xl font-extrabold tracking-tight 
					bg-gradient-to-r ${gradients[index]} 
					bg-clip-text text-transparent drop-shadow-md 
					transition-all duration-1000 ease-in-out select-none`}
      >
        {activeView.toUpperCase()}
      </h1>

      {/* Conditional Spinner Overlay for the entire file view */}



      {viewMode === "list" ? (
        <div className="space-y-1">
          <div className="grid grid-cols-[auto_1fr_120px_120px_auto] gap-4 px-4 py-2 text-sm text-muted-foreground border-b">
            <div className="w-6"></div>
            <div>Name</div>
            <div>Owner</div>
            <div>Last modified</div>
            <div>File size</div>
          </div>

          {sortedFiles.map((file) => {
            const isSelected = selectedFiles.includes(file.id);
            const isFileActionLoading = fileActionLoading === file.id;

            return (
              <div
                key={file.id}
                className={cn(
                  "grid grid-cols-[auto_1fr_120px_120px_auto_40px] gap-4 px-4 py-3 hover:bg-muted/50 rounded-lg transition-smooth group cursor-pointer relative", // Added 'relative' and extra column
                  isSelected && "bg-muted/50"
                )}
                onClick={() => toggleFileSelection(file.id)}
              >
                <Checkbox
                  checked={isSelected}
                  onChange={() => toggleFileSelection(file.id)}
                  className="mt-2 opacity-100 group-hover:opacity-100 transition-smooth"
                />

                <div className="flex items-center gap-3 min-w-0">
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
                
                {/* LIST VIEW ACTIONS COLUMN */}
                <div className="flex justify-end items-center">
                    {isFileActionLoading ? (
                        <Spinner className="w-4 h-4 text-primary" />
                    ) : (
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button
                              variant="ghost"
                              size="sm"
                              className="h-6 w-6 p-0 opacity-0 group-hover:opacity-100 transition-smooth"
                              onClick={(e) => e.stopPropagation()}
                            >
                              <MoreVertical className="w-4 h-4" />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end">
                            <DropdownMenuItem 
                              onClick={(e) => { e.stopPropagation(); handleToggleStar(file.id); }}
                            >
                              <Star className={cn("w-4 h-4 mr-2", file.starred ? "text-drive-yellow fill-current" : "text-muted-foreground")} />
                              {file.starred ? "Unstar" : "Star"}
                            </DropdownMenuItem>
                            <DropdownMenuItem>
                              <Share2 className="w-4 h-4 mr-2" />
                              Share
                            </DropdownMenuItem>
                            <DropdownMenuItem>
                              <Download className="w-4 h-4 mr-2" />
                              Download
                            </DropdownMenuItem>
                            <DropdownMenuSeparator />
                            <DropdownMenuItem
                              className="text-destructive"
                              onClick={(e) => {
                                e.stopPropagation();
                                handleMoveToTrash(file);
                              }}
                            >
                              <Trash2 className="w-4 h-4 mr-2" />
                              Move to trash
                            </DropdownMenuItem>
                          </DropdownMenuContent>
                        </DropdownMenu>
                    )}
                </div>
              </div>
            );
          })}
          <FileActionsToolbar />
        </div>
      ) : (
        <div className="relative">
          {" "}
          {/* ADD relative HERE */}
          <div className="absolute inset-0 z-0 overflow-hidden rounded-lg pointer-events-none">
            {/* SplashCursor... */}
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4 p-4 relative z-10">
            {sortedFiles.map((file) => {
              const isSelected = selectedFiles.includes(file.id);
              const isFileActionLoading = fileActionLoading === file.id;

              return (
                <Card
                  key={file.id}
                  className={cn(
                    "p-4 hover:shadow-hover transition-smooth cursor-pointer group animate-scale-in  relative z-10",
                    isSelected && "ring-2 ring-primary"
                  )}
                  onClick={() => toggleFileSelection(file.id)}
                >
                  <div className="flex items-start justify-between mb-3">
                    <FilePreview file={file} size="lg" />

                    <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-smooth">
                      <Button
                        variant="ghost"
                        size="sm"
                        className="h-6 w-6 p-0"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleToggleStar(file.id); // Use the new handler
                        }}
                        disabled={isFileActionLoading} // Disable when another action is loading
                      >
                         {isFileActionLoading && file.starred ? (
                            <Spinner className="w-4 h-4 text-drive-yellow" />
                         ) : (
                            <Star
                              className={cn(
                                "w-4 h-4",
                                file.starred
                                  ? "text-drive-yellow fill-current"
                                  : "text-muted-foreground"
                              )}
                            />
                         )}
                      </Button>

                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button
                            variant="ghost"
                            size="sm"
                            className="h-6 w-6 p-0"
                            onClick={(e) => e.stopPropagation()}
                          >
                            <MoreVertical className="w-4 h-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem>
                            <Share2 className="w-4 h-4 mr-2" />
                            Share
                          </DropdownMenuItem>
                          <DropdownMenuItem>
                            <Download className="w-4 h-4 mr-2" />
                            Download
                          </DropdownMenuItem>
                          <DropdownMenuSeparator />
                          {/* Use DropdownMenuItem directly for the trash action for simpler styling */}
                          <DropdownMenuItem
                            className="text-destructive"
                            onClick={(e) => {
                              // Prevent the event from bubbling up to the Card, which would trigger selection
                              e.stopPropagation();
                              // Call the function with the current file object
                              handleMoveToTrash(file);
                            }}
                            disabled={isFileActionLoading}
                          >
                            {isFileActionLoading && !file.starred ? (
                                <Spinner className="w-4 h-4 mr-2 text-destructive" />
                            ) : (
                                <Trash2 className="w-4 h-4 mr-2" />
                            )}
                            Move to trash
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </div>
                  </div>

                  <div className="space-y-1">
                    <h3 className="font-medium text-sm text-foreground truncate">
                      {file.fileName ?? "Unnamed"}
                    </h3>
                    <div className="flex items-center gap-2 text-xs text-muted-foreground">
                      <span>•</span>
                      <span>{formatDate(file.processedAt)}</span>
                      {file.size && (
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
      )}
    </div>
  );
}