import { useCallback, useState } from 'react';
import { Upload, FileText, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Progress } from '@/components/ui/progress';
import { useToast } from '@/hooks/use-toast';
import { useDriveStore } from '@/stores/driveStore';
import axios from 'axios'; // Import axios for file uploads
// --- NEW IMPORT ---
import SplashCursor from './SplashCursor'; // Assuming this component is available

interface UploadFile {
  id: string;
  name: string;
  size: number;
  progress: number;
  status: 'uploading' | 'completed' | 'error';
}
interface ProcessedDocument {
    id: string; // Assuming the server returns the file ID
    fileName: string;
    fileType: string;
    fileSize: number;
    s3Location: string;
    securityStatus: string;
    rejectionReason: string | null;
    userId: string;
    processedAt: string; // Necessary for FileGrid sorting/display
    isStarred: boolean; // Assuming the server returns the starred status
    thumbnailUrl?: string;
    // ... other metadata like tags, categories, summary
}


export function UploadZone() {
// ... (All logic, state, and handler functions remain the same) ...

  const [isDragOver, setIsDragOver] = useState(false);
  const [uploadFiles, setUploadFiles] = useState<UploadFile[]>([]);
  const { toast } = useToast();
  const { addFile } = useDriveStore();
  const token = localStorage.getItem("auth_token") || null;
  const user = localStorage.getItem("auth_user") || null;

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(true);
  }, []);

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
  }, []);

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);

    const files = Array.from(e.dataTransfer.files);
    handleFileUpload(files);
  }, []);

  const handleFileSelect = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files || []);
    handleFileUpload(files);
    e.target.value = '';
  }, []);

  const removeUploadFile = useCallback((id: string) => {
    setUploadFiles(prev => prev.filter(f => f.id !== id));
  }, []);

  const handleFileUpload = useCallback(async (files: File[]) => {
    // Convert File objects into a new format for UI display
    const newUploadFiles: UploadFile[] = files.map(file => ({
      id: Math.random().toString(36).substr(2, 9),
      name: file.name,
      size: file.size,
      progress: 0,
      status: 'uploading' as const,
    }));

    // Add new files to the upload list
    setUploadFiles(prev => [...prev, ...newUploadFiles]);

    // Process each file for upload
    files.forEach(async (file, index) => {
      const uploadFile = newUploadFiles[index];
      const formData = new FormData();
      formData.append('file', file); // 'file' should match the field name expected by the server

      try {
        // ... (axios post request remains the same)
        const response = await axios.post<ProcessedDocument>('http://localhost:8083/api/genai/process', formData, {
          // ... (headers and onUploadProgress remain the same)
            headers: {
                'Content-Type': 'multipart/form-data',
                'Authorization': `Bearer ${token}`,
            },
            withCredentials: true,
            onUploadProgress: (progressEvent) => {
                if (progressEvent.total) {
                    const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total);
                    setUploadFiles(prev =>
                        prev.map(f =>
                            f.id === uploadFile.id
                                ? { ...f, progress }
                                : f
                        )
                    );
                }
            },
        });

        const serverData = response.data; // Alias server data for clarity
        console.log(serverData)

        // --- Handle Server Rejection (remains the same) ---
        if (serverData.securityStatus !== "safe") {
          // ... (rejection logic remains the same)
            const rejectionReason = serverData.rejectionReason || 'File rejected by server policy.';
            setUploadFiles(prev => 
                prev.map(f => 
                    f.id === uploadFile.id 
                        ? { ...f, progress: 100, status: 'error' as const }
                        : f
                )
            );
            toast({
                title: "Upload rejected",
                description: `${uploadFile.name} was rejected. Reason: ${rejectionReason}`,
                variant: "destructive"
            });
            setTimeout(() => {
                removeUploadFile(uploadFile.id);
            }, 4000);
            return;
        }


        // --- Handle Successful Upload (securityStatus === "safe") ---
        
        // 1. Update the temporary upload file entry (optional, but good practice)
        setUploadFiles(prev =>
          prev.map(f =>
            f.id === uploadFile.id
              ? { 
                    ...f, 
                    progress: 100, 
                    status: 'completed' as const, 
                    name: serverData.fileName // Use server's name in upload list
                }
              : f
          )
        );

        // 2. Add the file to the drive store using SERVER DATA
        addFile({
          id: serverData.id, 
          fileName: serverData.fileName, 
          name: serverData.fileName, 
          type: serverData.fileType, 
          size: serverData.fileSize, 
          processedAt: serverData.processedAt || new Date().toISOString(), 
          modifiedTime: serverData.processedAt || new Date().toISOString(),
          starred: serverData.isStarred || false, 
          shared: false,
          owner: 'me',
        });


        toast({
          title: "Upload completed",
          description: `${serverData.fileName} has been uploaded and added to your drive.`,
        });

        // Remove the file from the UI after a short delay (remains the same)
        setTimeout(() => {
          removeUploadFile(uploadFile.id);
        }, 300);

      } catch (error) {
        // ... (error handling remains the same)
        console.error("Upload error:", error);
        setUploadFiles(prev =>
            prev.map(f =>
                f.id === uploadFile.id
                    ? { ...f, progress: 0, status: 'error' as const }
                    : f
            )
        );
        const errorMessage = error instanceof Error ? error.message : "An unknown error occurred.";
        toast({
            title: "Upload failed",
            description: `Failed to upload ${uploadFile.name}. ${errorMessage}`,
            variant: "destructive"
        });
        setTimeout(() => {
            removeUploadFile(uploadFile.id);
        }, 4000);
      }
    });
  }, [addFile, toast, token, user, removeUploadFile]); // Dependencies remain the same
  
  return (
    <div className="space-y-4">
        {/* Wrapper for the Card to enable relative positioning */}
        <div className="relative rounded-xl overflow-hidden">
            {/* The low-end SplashCursor component positioned absolutely */}
            <div className="absolute inset-0 z-0 pointer-events-none">
{/* <SplashCursor
    SIM_RESOLUTION={64}       // Keep low for performance.
    DYE_RESOLUTION={256}      // Keep low for performance.
    
    // --- Adjustments for Small, Subtle, and Sweet Effect ---
    
    // DENSITY: Make the trails fade much faster (Crucial for subtlety)
    DENSITY_DISSIPATION={0.995} // Very close to 1.0, fades almost instantly.
    
    // VELOCITY: Keep fast dissipation for quick settling.
    VELOCITY_DISSIPATION={10} 
    
    // PRESSURE: Use low pressure for a contained, gentle push.
    PRESSURE={0.3}            
    
    // CURL: Keep low for smooth, gentle swirling.
    CURL={15}                 
    
    // SPLAT_RADIUS: **CRITICAL: Make the brush strokes very small.**
    SPLAT_RADIUS={0.09}        
    
    // SPLAT_FORCE: Use low force so movement is gentle.
    SPLAT_FORCE={1500}        
    
    SHADING={false}           // Keep shading off.
    
    // COLOR SPEED: Medium-slow speed for gentle color blending.
    COLOR_UPDATE_SPEED={0.01}  
    
    BACK_COLOR={{ r: 0.0, g: 0.0, b: 0.0 }} 
    TRANSPARENT={true}        
/> */}
            </div>

            <Card
                className={`
                    border-2 border-dashed p-8 text-center transition-smooth cursor-pointer
                    relative z-10 // Ensure card content is above the cursor effect
                    ${isDragOver 
                        ? 'border-primary/50 bg-primary/5' 
                        : 'bg-background' // Use bg-background to cover the cursor on non-hover
                    }
                `}
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onDrop={handleDrop}
                onClick={() => document.getElementById('file-upload')?.click()}
            >
                <div className="space-y-4">
                    <div className={`mx-auto w-16 h-16 rounded-full flex items-center justify-center transition-smooth ${
                        isDragOver ? 'bg-primary text-white' : 'bg-muted'
                    }`}>
                        <Upload className="w-8 h-8" />
                    </div>
                    
                    <div className="space-y-2">
                        <h3 className="text-lg font-medium">
                            {isDragOver ? 'Drop files here' : 'Drag files here to upload'}
                        </h3>
                        <p className="text-muted-foreground">
                            or <span className="text-primary font-medium">browse files</span> from your computer
                        </p>
                    </div>
                </div>
                
                <input
                    id="file-upload"
                    type="file"
                    multiple
                    className="hidden"
                    onChange={handleFileSelect}
                />
            </Card>
        </div>

      {uploadFiles.length > 0 && (
        <div className="space-y-2">
          <h4 className="text-sm font-medium text-foreground">Uploading files</h4>
          
          {uploadFiles.map((file) => (
            <Card 
                key={file.id} 
                className={`p-3 ${file.status === 'error' ? 'border-destructive/70 bg-destructive/5' : ''}`}
            >
              <div className="flex items-center gap-3">
                <FileText className={`w-8 h-8 flex-shrink-0 ${file.status === 'error' ? 'text-destructive' : 'text-drive-blue'}`} />
                
                <div className="flex-1 min-w-0 space-y-1">
                  <div className="flex items-center justify-between">
                    <span className="text-sm font-medium truncate">{file.name}</span>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="h-6 w-6 p-0"
                      onClick={() => removeUploadFile(file.id)}
                    >
                      <X className="w-4 h-4" />
                    </Button>
                  </div>
                  
                  <div className="space-y-1">
                    {file.status === 'error' ? (
                        <p className='text-xs text-destructive font-medium'>Upload Error/Rejected</p>
                    ) : (
                        <>
                            <Progress 
                              value={file.progress} 
                              className="h-1" 
                            />
                            <div className="flex justify-between text-xs text-muted-foreground">
                              <span>{Math.round(file.progress)}%</span>
                              <span>{(file.size / 1024 / 1024).toFixed(1)} MB</span>
                            </div>
                        </>
                    )}
                  </div>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}