import { create } from 'zustand';
import axios from 'axios';

export interface DriveFile {
  id: string;
  fileName: string;
  fileType: string;
  processedAt: string;
  name: string;
  modifiedTime: string;
  size?: number;
  starred?: boolean;
  shared?: boolean;
  owner?: string;
  parentId?: string;
  thumbnailUrl?: string;
}

export interface User {
  id: number;
  username: string;
  email: string;
  picture: string;
}

interface AuthState {
  user: User | null;
  token: string | null;
  setUser: (user: User | null) => void;
  setToken: (token: string | null) => void;
  fetchUser: () => Promise<void>;
}

export interface DriveState {
  files: DriveFile[];
  currentFolder: string | null;
  selectedFiles: string[];
  viewMode: 'grid' | 'list';
  activeView: 'my-drive' | 'shared-with-me' | 'recent' | 'starred' | 'trash';
  searchQuery: string;
  fileTypeFilter: 'all' | 'folders' | 'documents' | 'images' | 'videos' | 'presentations' | 'spreadsheets';
  dateFilter: 'all' | 'today' | 'week' | 'month' | 'year';
  isLoading: boolean;
  user: {
    name: string;
    email: string;
    avatar: string;
  } | null;
  storageUsed: number;
  storageTotal: number;
}

export interface DriveActions {
  setFiles: (files: DriveFile[]) => void;
  setCurrentFolder: (folderId: string | null) => void;
  setSelectedFiles: (fileIds: string[]) => void;
  setActiveView: (view: DriveState['activeView']) => void;
  toggleFileSelection: (fileId: string) => void;
  setViewMode: (mode: 'grid' | 'list') => void;
  setSearchQuery: (query: string) => void;
  setFileTypeFilter: (filter: DriveState['fileTypeFilter']) => void;
  setDateFilter: (filter: DriveState['dateFilter']) => void;
  setLoading: (loading: boolean) => void;
  setUser: (user: DriveState['user']) => void;
  addFile: (file: DriveFile) => void;
  removeFile: (fileId: string) => void;
  updateFile: (fileId: string, updates: Partial<DriveFile>) => void;
   toggleStar: (fileId: string) => void;
  fetchFiles: (query: string) => Promise<void>; // Consolidated into a single fetch action
  fetchRecycledFiles: (query: string) => Promise<void>; // Consolidated into a single fetch action
  MoveFileToTrash: (filesToTrash: DriveFile[]) => Promise<void>; // Add this line
  DeleteFilesPermanently: (filesToTrash: DriveFile[]) => Promise<void>; 
  RestoreFiles: (filesToRestore: DriveFile[]) => Promise<void>;  // Consolidated into a single fetch action
  DownloadFiles: (filesToDownload: DriveFile[]) => Promise<void>;
  toggleStarStatus: (fileId: string, isStarred: boolean) => Promise<void>;
  fetchStarredFiles: () => Promise<void>;
  fetchRecentFiles:()=>Promise<void>;
}

export const useDriveStore = create<DriveState & DriveActions>((set, get) => ({
  files: [],
  activeView: 'my-drive', // Set a default view
  currentFolder: null,
  selectedFiles: [],
  viewMode: 'grid',
  searchQuery: '',
  fileTypeFilter: 'all',
  dateFilter: 'all',
  isLoading: false,
  user: null,
  storageUsed: 8589934592, // 8GB in bytes
  storageTotal: 16106127360, // 15GB in bytes
    setActiveView: (view) => {
      set({ activeView: view });
      // When the view changes, trigger the appropriate fetch action
      const { fetchFiles, fetchRecycledFiles, fetchStarredFiles, fetchRecentFiles } = get(); // Add fetchRecentFiles
      if (view === 'trash') {
        fetchRecycledFiles(get().searchQuery);
      } else if (view === 'my-drive') {
        fetchFiles(get().searchQuery);
      } else if (view === 'starred') {
        fetchStarredFiles();
      } else if (view === 'recent') { // NEW CONDITION ADDED HERE
        fetchRecentFiles();
      }
    },

 fetchRecentFiles: async () => {
    set({ isLoading: true });
    try {
      const { token } = useAuthStore.getState();
      const res = await axios.get('http://localhost:8085/api/metadata/user/recent', { // Use a clean 'recent' endpoint
        headers: { Authorization: `Bearer ${token}` },
      });

      const filesFromBackend = res.data;
      const mappedFiles = filesFromBackend.map((file: any) => ({
        id: file.id,
       fileName: file.fileName,
        fileType: file.fileType,
        processedAt: file.processedAt,
        name: file.fileName,
        modifiedTime: file.modifiedAt,
        shared: false,
        owner: 'You',
        size: file.fileSize,
        // FIX: Map the actual star status from the backend or default to false
        starred: file.isStarred || false, 
      }));

      set({ files: mappedFiles, selectedFiles: [] });
    } catch (error) {
      console.error("Failed to fetch recent files: ", error); // Updated log message
      set({ files: [] });
    } finally {
      set({ isLoading: false });
    }
  },

  fetchStarredFiles: async () => {
    set({ isLoading: true });
    try {
      const { token } = useAuthStore.getState();
      const res = await axios.get('http://localhost:8085/api/metadata/user/starred', {
        headers: { Authorization: `Bearer ${token}` },
      });

      const filesFromBackend = res.data;
      const mappedFiles = filesFromBackend.map((file: any) => ({
        id: file.id,
        fileName: file.fileName,
        fileType: file.fileType,
        processedAt: file.processedAt,
        name: file.fileName,
        modifiedTime: file.processedAt,
        shared: false,
        owner: 'You',
        size: file.fileSize,
        starred: true, // All files from this endpoint are starred
      }));

      set({ files: mappedFiles, selectedFiles: [] });
    } catch (error) {
      console.error("Failed to fetch starred files: ", error);
      set({ files: [] });
    } finally {
      set({ isLoading: false });
    }
  },


  setFiles: (files) => set({ files }),
  setCurrentFolder: (folderId) => set({ currentFolder: folderId }),
  setSelectedFiles: (fileIds) => set({ selectedFiles: fileIds }),
  toggleFileSelection: (fileId) => {
    const { selectedFiles } = get();
    const newSelection = selectedFiles.includes(fileId)
      ? selectedFiles.filter(id => id !== fileId)
      : [...selectedFiles, fileId];
    set({ selectedFiles: newSelection });
  },
  setViewMode: (mode) => set({ viewMode: mode }),
  setFileTypeFilter: (filter) => set({ fileTypeFilter: filter }),
  setDateFilter: (filter) => set({ dateFilter: filter }),
  setLoading: (loading) => set({ isLoading: loading }),
  setUser: (user) => set({ user }),
  addFile: (file) => set((state) => ({ files: [...state.files, file] })),
  removeFile: (fileId) => set((state) => ({ 
    files: state.files.filter(f => f.id !== fileId),
    selectedFiles: state.selectedFiles.filter(id => id !== fileId)
  })),
  updateFile: (fileId, updates) => set((state) => ({
    files: state.files.map(f => f.id === fileId ? { ...f, ...updates } : f)
  })),


  toggleStarStatus: async (fileId, isStarred) => {
    try {
      const { token } = useAuthStore.getState();
      const res = await axios.post(
        `http://localhost:8082/api/star/${fileId}`,
        isStarred,
        {
          headers: {
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json',
          },
        }
      );
      if (res.data) {
        set((state) => ({
          files: state.files.map((file) =>
            file.id === fileId ? { ...file, starred: isStarred } : file
          ),
        }));
      } else {
        console.error("Failed to update star status on the backend.");
      }
    } catch (error) {
      console.error("Failed to update star status: ", error);
    }
  },
    toggleStar: (fileId) => {
    const { files, toggleStarStatus } = get();
    const file = files.find((f) => f.id === fileId);
    if (file) {
      toggleStarStatus(fileId, !file.starred);
    }
  },

// Inside useDriveStore
MoveFileToTrash: async (filesToTrash: DriveFile[]) => {
  set({ isLoading: true });
  try {
    const { token } = useAuthStore.getState();
    const fileIds = filesToTrash.map(file => file.id);

    // Make a DELETE request to your backend with the file IDs
    console.log("tried to called MoveToRecycleBin")
    const res = await axios.delete('http://localhost:8082/api/MoveToRecycleBin', {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      data: fileIds
    });
    console.log(res.data)

    // On successful deletion, update the local state by removing the files
    set((state) => ({
      files: state.files.filter(file => !fileIds.includes(file.id)),
      selectedFiles: [] // Clear selected files after moving them
    }));
  } catch (error) {
    console.error("Failed to move files to trash: ", error);
    // Handle error, e.g., show a toast notification
  } finally {
    set({ isLoading: false });
  }
}, 

  RestoreFiles: async (filesToRestore: DriveFile[]) => {
    set({ isLoading: true });
    try {
          const { token } = useAuthStore.getState();
       const { fetchRecycledFiles } = get();
      const fileIds = filesToRestore.map(file => file.id);

      await axios.post('http://localhost:8082/api/RestoreFiles', fileIds, {
        headers: { 'Authorization': `Bearer ${token}` },
      });

      // After a successful restore, refresh the trash view to show the files are gone.
      fetchRecycledFiles(get().searchQuery);
      set({ selectedFiles: [] });
    } catch (error) {
      console.error("Failed to restore files: ", error);
    } finally {
      set({ isLoading: false });
    }
  },

  DeleteFilesPermanently: async (filesToDelete: DriveFile[]) => {
    set({ isLoading: true });
    try {
      const { token } = useAuthStore.getState();
       const { fetchRecycledFiles } = get();
      const fileIds = filesToDelete.map(file => file.id);

      await axios.delete('http://localhost:8082/api/PermanentlyDeleteFiles', {
        headers: { 'Authorization': `Bearer ${token}` },
        data: fileIds, // Use 'data' for DELETE requests with a body
      });

      // After permanent deletion, refresh the trash view
      fetchRecycledFiles(get().searchQuery);
      set({ selectedFiles: [] });
    } catch (error) {
      console.error("Failed to permanently delete files: ", error);
    } finally {
      set({ isLoading: false });
    }
  },

  UpdateStarStatus: async ()=>{
    // Enter logic here
  },

DownloadFiles: async (filesToDownload: DriveFile[]) => {
  set({ isLoading: true });
  try {
    const { token } = useAuthStore.getState();
    const fileIds = filesToDownload.map((file) => file.id);

    const res = await axios.post(
      'http://localhost:8082/api/DownloadFiles',
      fileIds,
      {
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        responseType: 'blob', // Crucial for handling binary data like a zip file
      }
    );

    // Create a URL for the blob and trigger a download
    const url = window.URL.createObjectURL(new Blob([res.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', 'files.zip'); // Set the file name
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    // Clean up the URL
    window.URL.revokeObjectURL(url);
    set({ selectedFiles: [] }); // Clear selection after download
  } catch (error) {
    console.error('Failed to download files: ', error);
    // You can add a toast or a user-friendly message here
  } finally {
    set({ isLoading: false });
  }
},




fetchFiles: async (query = '') => {
    set({ isLoading: true });
    try {
      const { token } = useAuthStore.getState();
      let url = "http://localhost:8085/api/metadata/user/search";
      let params = {};

      // If a query is provided, use the search endpoint
      if (query.trim() !== '') {
        url = "http://localhost:8085/api/metadata/search";
        params = { query: query };
      }

      const res = await axios.get(url, {
        headers: { Authorization: `Bearer ${token}` },
        params: params,
        withCredentials: true,
      });

      console.log(res.data)

      const filesFromBackend = res.data;
      const mappedFiles: DriveFile[] = filesFromBackend.map((file: any) => ({
        id: file.id,
        fileName: file.fileName,
        fileType: file.fileType,
        processedAt: file.processedAt,
        name: file.fileName,
        modifiedTime: file.processedAt,
        starred: file.isStarred,
        shared: false,
        owner: 'You',
        size: file.fileSize,
      }));

      set({ files: mappedFiles });
    } catch (error) {
      console.error("Failed to fetch files: ", error);
      // It's also a good practice to set files to an empty array on error
      set({ files: [] });
    } finally {
      set({ isLoading: false });
    }
  },
fetchRecycledFiles: async (query = '') => {
    set({ isLoading: true });
    try {
      const { token } = useAuthStore.getState();
      let url = "http://localhost:8085/api/metadata/user/trash";
      let params = {};

      // If a query is provided, use the search endpoint
      if (query.trim() !== '') {
        url = "http://localhost:8085/api/metadata/search/trash";
        params = { query: query };
      }

      const res = await axios.get(url, {
        headers: { Authorization: `Bearer ${token}` },
        params: params,
        withCredentials: true,
      });

      console.log(res.data)

      const filesFromBackend = res.data;
      const mappedFiles: DriveFile[] = filesFromBackend.map((file: any) => ({
        id: file.id,
        fileName: file.fileName,
        fileType: file.fileType,
        processedAt: file.processedAt,
        name: file.fileName,
        modifiedTime: file.processedAt,
        starred: false,
        shared: false,
        owner: 'You',
        size: file.fileSize,
      }));

      set({ files: mappedFiles });
    } catch (error) {
      console.error("Failed to fetch files: ", error);
      // It's also a good practice to set files to an empty array on error
      set({ files: [] });
    } finally {
      set({ isLoading: false });
    }
  },

setSearchQuery: (query) => set({ searchQuery: query }),

}));

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  token: localStorage.getItem('auth_token') || null,
  
  setUser: (user) => {
    if (user) {
      localStorage.setItem('auth_user', JSON.stringify(user));
    } else {
      localStorage.removeItem('auth_user');
    }
    set({ user });
  },
  setToken: (token) => {
    if (token) {
      localStorage.setItem('auth_token', token);
    } else {
      localStorage.removeItem('auth_token');
    }
    set({ token });
  },
  fetchUser: async () => {
    const { token } = get();
    const storedToken = localStorage.getItem('auth_token');
    const currentToken = token || storedToken;

    if (!currentToken) {
      set({ user: null });
      localStorage.removeItem('auth_user');
      return;
    }

    try {
      const res = await axios.get<User>('http://localhost:8080/api/auth/user', {
        headers: {
          Authorization: `Bearer ${currentToken}`
        }
      });
      console.log(res.data);
      set({ user: res.data });
      localStorage.setItem('auth_user', JSON.stringify(res.data));
    } catch (err) {
      console.error("Token invalid or expired", err);
      set({ user: null });
      localStorage.removeItem('auth_user');
      localStorage.removeItem('auth_token');
    }
  }
}));