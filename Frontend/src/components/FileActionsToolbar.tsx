// components/FileActionsToolbar.tsx
import { Trash2, Download, Undo2, X } from "lucide-react"; // Import Undo2 icon
import { Button } from "@/components/ui/button";
import { useDriveStore } from "@/stores/driveStore";
import { cn } from "@/lib/utils";
import { motion, AnimatePresence } from "framer-motion";

export function FileActionsToolbar() {
  const { 
    selectedFiles, 
    files, 
    activeView, // Get the active view from the store
    MoveFileToTrash, 
    DeleteFilesPermanently, // Destructure the new actions
    RestoreFiles, 
    setSelectedFiles,
    DownloadFiles 
  } = useDriveStore();

  // Handler functions for different actions
  const handleMoveToTrash = () => {
    if (selectedFiles.length > 0) {
      const filesToTrash = files.filter((file) => selectedFiles.includes(file.id));
      MoveFileToTrash(filesToTrash);
    }
  };

  const handlePermanentDelete = () => {
    if (selectedFiles.length > 0) {
      const filesToDelete = files.filter((file) => selectedFiles.includes(file.id));
      DeleteFilesPermanently(filesToDelete);
    }
  };

  const handleRestore = () => {
    if (selectedFiles.length > 0) {
      const filesToRestore = files.filter((file) => selectedFiles.includes(file.id));
      RestoreFiles(filesToRestore);
    }
  };

  const handleDownload = () => {
    if (selectedFiles.length > 0) {
      // Find the DriveFile objects corresponding to the selected IDs
      const filesToDownload = files.filter((file) =>
        selectedFiles.includes(file.id)
      );
      // Call the store action
      DownloadFiles(filesToDownload);
    }
  };

  return (
    <AnimatePresence>
      {selectedFiles.length > 0 && (
        <motion.div
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
            >
              <X className="w-4 h-4 text-muted-foreground" />
            </Button>
          </div>
          <div className="flex items-center gap-2">
            {activeView === 'trash' ? (
              <>
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={handleRestore}
                  aria-label="Restore selected files"
                  className="text-muted-foreground hover:text-green-500"
                >
                  <Undo2 className="w-5 h-5" />
                </Button>
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={handlePermanentDelete}
                  aria-label="Permanently delete selected files"
                  className="text-muted-foreground hover:text-destructive"
                >
                  <Trash2 className="w-5 h-5" />
                </Button>
              </>
            ) : (
              <>
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={handleDownload}
                  aria-label="Download selected files"
                  className="text-muted-foreground hover:text-primary"
                >
                  <Download className="w-5 h-5" />
                </Button>
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={handleMoveToTrash}
                  aria-label="Move selected files to trash"
                  className="text-muted-foreground hover:text-destructive"
                >
                  <Trash2 className="w-5 h-5" />
                </Button>
              </>
            )}
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}