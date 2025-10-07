import {
  MoreVertical,
  Star,
  Share2,
  Download,
  Trash2
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
import { useEffect } from "react";
import { useDebounce } from "@/useDebounce";
import { FileActionsToolbar } from "./FileActionsToolbar";

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
  const diffTime = Math.abs(now.getTime() - date.getTime());
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

  if (diffDays === 1) return "Yesterday";
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
    activeView, // Add activeView here
     fetchStarredFiles,
    fetchRecycledFiles
  } = useDriveStore();

  const handleMoveToTrash = (fileToTrash: DriveFile) => {
  // Call the store action with the single file in an array
  MoveFileToTrash([fileToTrash]);
};

  const debouncedSearchQuery = useDebounce(searchQuery, 500);
  
 useEffect(() => {
    if (activeView === 'trash') {
      fetchRecycledFiles(debouncedSearchQuery);
    } else if (activeView === 'starred') {
      fetchStarredFiles();
    } else {
      fetchFiles(debouncedSearchQuery);
    }
  }, [activeView, debouncedSearchQuery, fetchFiles, fetchRecycledFiles, fetchStarredFiles]);


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

  if (viewMode === "list") {
    return (
      <div className="space-y-1">
        <div className="grid grid-cols-[auto_1fr_120px_120px_auto] gap-4 px-4 py-2 text-sm text-muted-foreground border-b">
          <div className="w-6"></div>
          <div>Name</div>
          <div>Owner</div>
          <div>Last modified</div>
          <div>File size</div>
        </div>

        {filteredFiles.map((file) => {
          const isSelected = selectedFiles.includes(file.id);

          return (
            <div
              key={file.id}
              className={cn(
                "grid grid-cols-[auto_1fr_120px_120px_auto] gap-4 px-4 py-3 hover:bg-muted/50 rounded-lg transition-smooth group cursor-pointer",
                isSelected && "bg-drive-blue-light"
              )}
              onClick={() => toggleFileSelection(file.id)}
            >
              <Checkbox
                checked={isSelected}
                onChange={() => toggleFileSelection(file.id)}
                className="mt-0.5 opacity-0 group-hover:opacity-100 transition-smooth"
              />

              <div className="flex items-center gap-3 min-w-0">
                <FilePreview file={file} size="sm" />
                <span className="truncate text-foreground">{file.fileName ?? "Unnamed"}</span>
                {file.starred && <Star className="w-4 h-4 text-drive-yellow fill-current flex-shrink-0" />}
                {file.shared && <Share2 className="w-4 h-4 text-muted-foreground flex-shrink-0" />}
              </div>

              <div className="text-sm text-muted-foreground truncate">{file.owner || "You"}</div>
              <div className="text-sm text-muted-foreground">{formatDate(file.processedAt)}</div>
              <div className="text-sm text-muted-foreground">{formatFileSize(file.size)}</div>
            </div>
          );
        })}
      </div>
    );
  }

  return (
                  <div className="relative"> {/* ADD relative HERE */}
      {/* ADD SPLASHCURSOR FOR BACKGROUND EFFECT 
        Use subtle settings and absolute positioning
      */}
      <div className="absolute inset-0 z-0 overflow-hidden rounded-lg pointer-events-none">
        <SplashCursor
          SIM_RESOLUTION={256}      // Lowered resolution for performance
          DYE_RESOLUTION={1024}     // Lowered resolution for performance
          DENSITY_DISSIPATION={0.3}
          VELOCITY_DISSIPATION={0.8}
          PRESSURE={0.5}
          CURL={30}
          SPLAT_RADIUS={0.15}       // Smaller radius
          SPLAT_FORCE={4000}        // Lower force
          SHADING={false}
          COLOR_UPDATE_SPEED={2.5}
          BACK_COLOR={{ r: 0.0, g: 0.0, b: 0.0 }}
          TRANSPARENT={true}
        />
      </div>
    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4 p-4 relative z-10">
      {filteredFiles.map((file) => {
        const isSelected = selectedFiles.includes(file.id);

        return (

          <Card
            key={file.id}
            className={cn(
              "p-4 hover:shadow-hover transition-smooth cursor-pointer group animate-scale-in  relative z-10",
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
                    toggleStar(file.id);
                  }}
                >
                  <Star className={cn("w-4 h-4", file.starred ? "text-drive-yellow fill-current" : "text-muted-foreground")} />
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
                    <button
                      onClick={(e) => {
      // Prevent the event from bubbling up to the Card, which would trigger selection
      e.stopPropagation(); 
      // Call the function with the current file object
      handleMoveToTrash(file); 
  }}
                    >
                      <DropdownMenuItem className="text-destructive">
                      <Trash2 className="w-4 h-4 mr-2" />
                      Move to trash
                    </DropdownMenuItem>
                    </button>
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>
            </div>

            <div className="space-y-1">
              <h3 className="font-medium text-sm text-foreground truncate">{file.fileName ?? "Unnamed"}</h3>
              <div className="flex items-center gap-2 text-xs text-muted-foreground">
                <span>{formatDate(file.processedAt)}</span>
                {file.size && (
                  <>
                    <span>•</span>
                    <span>{formatFileSize(file.size)}</span>
                  </>
                )}
              </div>

              <div className="flex items-center gap-1 mt-2">
                {file.shared && (
                  <div className="flex items-center gap-1 text-xs text-muted-foreground">
                    <Share2 className="w-3 h-3" />
                    <span>Shared</span>
                  </div>
                )}
              </div>
            </div>
          </Card>
        );
      })}
      <FileActionsToolbar />
    </div>
    </div>
  );
}