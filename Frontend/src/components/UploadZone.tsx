import { useCallback, useState } from 'react';
import { Upload, FileText, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Progress } from '@/components/ui/progress';
import { useToast } from '@/hooks/use-toast';
import { useDriveStore } from '@/stores/driveStore';
import axios from 'axios';

interface UploadFile {
  id: string;
  name: string;
  size: number;
  progress: number;
  status: 'uploading' | 'completed' | 'error';
}

interface ProcessedDocument {
    id: string;
    fileName: string;
    fileType: string;
    fileSize: number;
    s3Location: string;
    securityStatus: string;
    rejectionReason: string | null;
    userId: string;
    processedAt: string;
    isStarred: boolean;
    thumbnailUrl?: string;
}

export function UploadZone() {
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
    const newUploadFiles: UploadFile[] = files.map(file => ({
      id: Math.random().toString(36).substr(2, 9),
      name: file.name,
      size: file.size,
      progress: 0,
      status: 'uploading' as const,
    }));

    setUploadFiles(prev => [...prev, ...newUploadFiles]);

    files.forEach(async (file, index) => {
      const uploadFile = newUploadFiles[index];
      const formData = new FormData();
      formData.append('file', file);

      try {
        const response = await axios.post<ProcessedDocument>(
          'http://localhost:8083/api/genai/process', 
          formData, 
          {
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
                    f.id === uploadFile.id ? { ...f, progress } : f
                  )
                );
              }
            },
          }
        );

        const serverData = response.data;

        if (serverData.securityStatus !== "safe") {
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
          setTimeout(() => removeUploadFile(uploadFile.id), 4000);
          return;
        }

        setUploadFiles(prev =>
          prev.map(f =>
            f.id === uploadFile.id
              ? { 
                  ...f, 
                  progress: 100, 
                  status: 'completed' as const, 
                  name: serverData.fileName
                }
              : f
          )
        );

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
          description: `${serverData.fileName} has been uploaded successfully.`,
        });

        setTimeout(() => removeUploadFile(uploadFile.id), 300);

      } catch (error) {
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
        setTimeout(() => removeUploadFile(uploadFile.id), 4000);
      }
    });
  }, [addFile, toast, token, removeUploadFile]);
  
  return (
    <div className="space-y-4">
      <div className="rounded-xl overflow-hidden">
        <Card
          className={`
            border-2 border-dashed p-8 text-center transition-all duration-200 cursor-pointer
            ${isDragOver 
              ? 'border-primary/50 bg-primary/5' 
              : 'border-border bg-background hover:border-primary/30'
            }
          `}
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          onClick={() => document.getElementById('file-upload')?.click()}
        >
          <div className="space-y-4">
            <div className={`mx-auto w-16 h-16 rounded-full flex items-center justify-center transition-all duration-200 ${
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
        <div className="space-y-3">
          <h4 className="text-sm font-medium text-foreground">Uploading files</h4>
          
          {uploadFiles.map((file) => (
            <Card 
              key={file.id} 
              className={`p-4 ${file.status === 'error' ? 'border-destructive/70 bg-destructive/5' : 'bg-card'}`}
            >
              <div className="flex items-center gap-3">
                <FileText className={`w-8 h-8 flex-shrink-0 ${
                  file.status === 'error' ? 'text-destructive' : 
                  file.status === 'completed' ? 'text-green-500' : 
                  'text-blue-500'
                }`} />
                
                <div className="flex-1 min-w-0 space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-sm font-medium truncate">{file.name}</span>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="h-6 w-6 p-0 ml-2"
                      onClick={() => removeUploadFile(file.id)}
                    >
                      <X className="w-4 h-4" />
                    </Button>
                  </div>
                  
                  <div className="space-y-1">
                    {file.status === 'error' ? (
                      <p className='text-xs text-destructive font-medium'>Upload Error/Rejected</p>
                    ) : file.status === 'completed' ? (
                      <p className='text-xs text-green-600 font-medium'>Upload Complete</p>
                    ) : (
                      <>
                        <Progress 
                          value={file.progress} 
                          className="h-1.5" 
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