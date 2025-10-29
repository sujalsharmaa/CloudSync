// components/FileActionsToolbar.tsx
import {
  Trash2,
  Download,
  Undo2,
  X,
  Loader2, // 1. Import Loader2
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { useDriveStore } from "@/stores/driveStore";
import { cn } from "@/lib/utils";
import { motion, AnimatePresence } from "framer-motion";
import { useState } from "react"; // 2. Import useState

// 3. Define a reusable Spinner component
const Spinner = () => <Loader2 className="w-5 h-5 animate-spin" />;

export function FileActionsToolbar() {
  const {
    selectedFiles,
    files,
    activeView,
    MoveFileToTrash,
    DeleteFilesPermanently,
    RestoreFiles,
    setSelectedFiles,
    DownloadFiles,
  } = useDriveStore();

  // 4. Add local state to track the loading action
  const [loadingAction, setLoadingAction] = useState<
    "trash" | "delete" | "restore" | "download" | null
  >(null);

  // 5. Update handlers to be async and set loading state
  const handleMoveToTrash = async () => {
    if (selectedFiles.length > 0) {
      setLoadingAction("trash");
      try {
        const filesToTrash = files.filter((file) =>
          selectedFiles.includes(file.id)
        );
        await MoveFileToTrash(filesToTrash);
      } catch (error) {
        console.error("Failed to move to trash", error);
        // Optionally show an error toast here
      } finally {
        setLoadingAction(null);
      }
    }
  };

  const handlePermanentDelete = async () => {
    if (selectedFiles.length > 0) {
      setLoadingAction("delete");
      try {
        const filesToDelete = files.filter((file) =>
          selectedFiles.includes(file.id)
        );
        await DeleteFilesPermanently(filesToDelete);
      } catch (error) {
        console.error("Failed to delete permanently", error);
      } finally {
        setLoadingAction(null);
      }
    }
  };

  const handleRestore = async () => {
    if (selectedFiles.length > 0) {
      setLoadingAction("restore");
      try {
        const filesToRestore = files.filter((file) =>
          selectedFiles.includes(file.id)
        );
        await RestoreFiles(filesToRestore);
      } catch (error) {
        console.error("Failed to restore", error);
      } finally {
        setLoadingAction(null);
      }
    }
  };

  const handleDownload = async () => {
    if (selectedFiles.length > 0) {
      setLoadingAction("download");
      try {
        const filesToDownload = files.filter((file) =>
          selectedFiles.includes(file.id)
        );
        await DownloadFiles(filesToDownload);
      } catch (error) {
        console.error("Failed to download", error);
      } finally {
        setLoadingAction(null);
      }
    }
  };

  const isAnyActionLoading = loadingAction !== null;

  return (
    <AnimatePresence>
      {selectedFiles.length > 0 && (
        <motion.div
          // ... motion props
          initial={{ opacity: 0, y: 50 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: 50 }}
          transition={{
            type: "spring",
            damping: 10,
            stiffness: 100,
          }}
          className={cn(
            "fixed bottom-4 left-1/2 -translate-x-1/2 p-1 border-white border-2",
            "bg-card text-card-foreground shadow-xl rounded-full",
            "flex items-center gap-2 z-50 transition-colors"
          )}
        >
          <div className="flex items-center ml-4 gap-2">
            <span className="text-sm font-medium">
              {selectedFiles.length} file{selectedFiles.length > 1 ? "s" : ""}{" "}
              selected
            </span>
            <Button
              variant="ghost"
              size="icon"
              onClick={() => setSelectedFiles([])}
              aria-label="Clear selection"
              disabled={isAnyActionLoading} // Disable clear when loading
            >
              <X className="w-4 h-4 text-muted-foreground" />
            </Button>
          </div>
          <div className="flex items-center gap-2">
            {activeView === "trash" ? (
              <>
                {/* 6. Update Restore Button */}
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={handleRestore}
                  aria-label="Restore selected files"
                  className="text-muted-foreground hover:text-green-500"
                  disabled={isAnyActionLoading}
                >
                  {loadingAction === "restore" ? (
                    <Spinner />
                  ) : (
                    <Undo2 className="w-5 h-5" />
                  )}
                </Button>
                {/* 7. Update Permanent Delete Button */}
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={handlePermanentDelete}
                  aria-label="Permanently delete selected files"
                  className="text-muted-foreground hover:text-destructive"
                  disabled={isAnyActionLoading}
                >
                  {loadingAction === "delete" ? (
                    <Spinner />
                  ) : (
                    <Trash2 className="w-5 h-5" />
                  )}
                </Button>
              </>
            ) : (
              <>
                {/* 8. Update Download Button */}
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={handleDownload}
                  aria-label="Download selected files"
                  className="text-muted-foreground hover:text-primary"
                  disabled={isAnyActionLoading}
                >
                  {loadingAction === "download" ? (
                    <Spinner />
                  ) : (
                    <Download className="w-5 h-5" />
                  )}
                </Button>
                {/* 9. Update Move to Trash Button */}
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={handleMoveToTrash}
                  aria-label="Move selected files to trash"
                  className="text-muted-foreground hover:text-destructive"
                  disabled={isAnyActionLoading}
                >
                  {loadingAction === "trash" ? (
                    <Spinner />
                  ) : (
                    <Trash2 className="w-5 h-5" />
                  )}
                </Button>
              </>
            )}
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}