import { useCallback, useState } from 'react';
import { Upload, FileText, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Progress } from '@/components/ui/progress';
import { useToast } from '@/hooks/use-toast';
import { useDriveStore } from '@/stores/driveStore';
import axios from 'axios'; // Import axios for file uploads

interface UploadFile {
  id: string;
  name: string;
  size: number;
  progress: number;
  status: 'uploading' | 'completed' | 'error';
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
        console.log(token)
        console.log(user)
        const response = await axios.post('http://localhost:8083/api/genai/process', formData, {
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

        // Handle successful upload
        if (response.status === 200) {
          setUploadFiles(prev => 
            prev.map(f => 
              f.id === uploadFile.id 
                ? { ...f, progress: 100, status: 'completed' as const }
                : f
            )
          );

          addFile({
            id: uploadFile.id, // Use the same ID for consistency
            name: uploadFile.name,
            type: 'file',
            size: uploadFile.size,
            modifiedTime: new Date().toISOString(),
            starred: false,
            shared: false,
            owner: 'me',
          });

          toast({
            title: "Upload completed",
            description: `${uploadFile.name} has been uploaded successfully.`,
          });

          // Remove the file from the UI after a delay
          setTimeout(() => {
            setUploadFiles(prev => prev.filter(f => f.id !== uploadFile.id));
          }, 3000);
        } else {
          // Handle non-200 responses
          throw new Error('Upload failed with status ' + response.status);
        }
      } catch (error) {
        console.error("Upload error:", error);
        setUploadFiles(prev => 
          prev.map(f => 
            f.id === uploadFile.id 
              ? { ...f, progress: 0, status: 'error' as const }
              : f
          )
        );
        toast({
          title: "Upload failed",
          description: `Failed to upload ${uploadFile.name}. Please try again.`,
          variant: "destructive"
        });
      }
    });
  }, [addFile, toast]);

  const removeUploadFile = useCallback((id: string) => {
    setUploadFiles(prev => prev.filter(f => f.id !== id));
  }, []);

  return (
    <div className="space-y-4">
      <Card
        className={`
          border-2 border-dashed p-8 text-center transition-smooth cursor-pointer
          ${isDragOver 
            ? 'border-primary bg-drive-blue-light' 
            : 'border-border hover:border-primary/50 hover:bg-muted/20'
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

      {uploadFiles.length > 0 && (
        <div className="space-y-2">
          <h4 className="text-sm font-medium text-foreground">Uploading files</h4>
          
          {uploadFiles.map((file) => (
            <Card key={file.id} className="p-3">
              <div className="flex items-center gap-3">
                <FileText className="w-8 h-8 text-drive-blue flex-shrink-0" />
                
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
                    <Progress 
                      value={file.progress} 
                      className="h-1" 
                    />
                    <div className="flex justify-between text-xs text-muted-foreground">
                      <span>{Math.round(file.progress)}%</span>
                      <span>{(file.size / 1024 / 1024).toFixed(1)} MB</span>
                    </div>
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