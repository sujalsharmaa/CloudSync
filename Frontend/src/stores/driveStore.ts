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
  //status: 'UPLOADED' | 'PROCESSING' | 'TAGS_GENERATED' | 'FAILED'; 
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
  // uploadSuccessFlag: Boolean;
  currentFolder: string | null;
  selectedFiles: string[];
  viewMode: 'grid' | 'list';
  fileActionLoading: Set<string>;
  activeView: 'my-drive' | 'shared-with-me' | 'recent' | 'starred' | 'trash';
  searchQuery: string;
  fileTypeFilter: 'all' | 'folders' | 'documents' | 'images' | 'videos' | 'presentations' | 'spreadsheets';
  dateFilter: 'all' | 'today' | 'week' | 'month' | 'year';
  isLoading: boolean;
  plan: string;
  user: {
    name: string;
    email: string;
    avatar: string;
  } | null;
  storageUsed: number;
  storageTotal: number;
}
// Define the expected structure of the successful response data
export interface PaymentSessionResponse {
  sessionUrl: string;
  // Add other properties if they exist, e.g., 'sessionId': string
}

export interface DriveActions {
  setFiles: (files: DriveFile[]) => void;
  // setUploadSuccessFlag: (status: Boolean)=>void;
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
  //connectWebSocket: (email: string) => void;
 toggleStar: (fileId: string) => Promise<void>; // Ensure this is async
  fetchFiles: (query: string) => Promise<void>; // Consolidated into a single fetch action
  fetchRecycledFiles: (query: string) => Promise<void>; // Consolidated into a single fetch action
  MoveFileToTrash: (filesToTrash: DriveFile[]) => Promise<void>; // Add this line
  DeleteFilesPermanently: (filesToTrash: DriveFile[]) => Promise<void>; 
  RestoreFiles: (filesToRestore: DriveFile[]) => Promise<void>;  // Consolidated into a single fetch action
  DownloadFiles: (filesToDownload: DriveFile[]) => Promise<void>;
  toggleStarStatus: (fileId: string, isStarred: boolean) => Promise<void>;
  fetchStarredFiles: () => Promise<void>;
  fetchRecentFiles:()=>Promise<void>;
  fetchUserStoragePlanAndConsumption:()=>Promise<void>
  handlePayment:(Plan:string,price: number)=>Promise<PaymentSessionResponse>
  
}

export const useDriveStore = create<DriveState & DriveActions>((set, get) => ({
  files: [],
  activeView: 'my-drive', // Set a default view
  currentFolder: null,
  selectedFiles: [],
  // uploadSuccessFlag: false,
  fileActionLoading: new Set<string>(),
  viewMode: 'grid',
  searchQuery: '',
  fileTypeFilter: 'all',
  dateFilter: 'all',
  isLoading: false,
  user: null,
  storageUsed: 0,
  storageTotal: 0,
  plan: 'DEFAULT',  
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
      const res = await axios.get(`${import.meta.env.VITE_PUBLIC_SEARCH_SERVICE}/api/metadata/user/recent`, { // Use a clean 'recent' endpoint
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


  fetchUserStoragePlanAndConsumption: async()=>{
    try {
            const { token } = useAuthStore.getState();
      const res = await axios.get(`${import.meta.env.VITE_PUBLIC_AUTH_SERVICE}/api/auth/getStoragePlanAndConsumption`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      console.log(res.data)


      let storageTotal = 0;
      const userPlan = res.data.plan; // <-- Store the plan locally

      switch (userPlan) {
        case "DEFAULT":
          storageTotal = 1024*1024*1024;
          break;
        case "BASIC":
          storageTotal = 1024*1024*1024*100;
          break;
        case "PRO":
          storageTotal = 1024*1024*1024*1000;
          break;
        case "TEAM":
          storageTotal = 1024*1024*1024*5000;
          break;
        default:
          storageTotal = 0;
          break;
      }

      const storageUsed = res.data.storageConsumed;
          set({
      storageTotal,
      storageUsed,
      plan: userPlan, // <-- SAVE THE PLAN TO THE STORE
    });

    } catch (error) {
      console.error("Failed to fetch storage plan: ", error);
    }
  },

  fetchStarredFiles: async () => {
    set({ isLoading: true });
    try {
      const { token } = useAuthStore.getState();
      const res = await axios.get(`${import.meta.env.VITE_PUBLIC_SEARCH_SERVICE}/api/metadata/user/starred`, {
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
  // In your driveStore
setSelectedFiles: (fileIds: string[]) => {
  set({ selectedFiles: fileIds });
},
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

  handlePayment: async (Plan: string, price: number): Promise<PaymentSessionResponse | undefined> => {
    try {
      const { token } = useAuthStore.getState();
             console.log(Plan)
        const res = await axios.post( `${import.meta.env.VITE_PUBLIC_PAYMENT_SERVICE}/service/v1/checkout`  ,
   
          {
            plan: Plan,
            amount: price
          },
        {
          headers: {
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json',
          },
        }
        )
        
        return res.data
    } catch (error) {
      console.error("Failed to handle payment ", error);
    }
  },

  toggleStarStatus: async (fileId, isStarred) => {
    try {
      const { token } = useAuthStore.getState();
      const res = await axios.post(
        `${import.meta.env.VITE_PUBLIC_FILE_SERVICE}/api/star/${fileId}`,
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
toggleStar: async (fileId: string) => {
    const { files, updateFile } = get();
    const originalFiles = [...files]; // Store original state for potential rollback
    const fileToUpdate = files.find((f) => f.id === fileId);

    if (!fileToUpdate) return;

    // 1. Add file ID to the loading set to show a spinner immediately
    set((state) => ({
      fileActionLoading: new Set(state.fileActionLoading).add(fileId),
    }))

    // 2. Optimistically update the UI without waiting for the server
    const newStarredStatus = !fileToUpdate.starred;
    updateFile(fileId, { starred: newStarredStatus });

    try {
      // 3. Make the API call in the background
      const { token } = useAuthStore.getState();
      await axios.post(
        `${import.meta.env.VITE_PUBLIC_FILE_SERVICE}/api/star/${fileId}`,
        newStarredStatus,
        {
          headers: {
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json',
          },
        }
      );
      // Success! The UI is already updated, so we don't need to do anything.
    } catch (error) {
      console.error("Failed to update star status, rolling back UI:", error);
      // 4. If the API call fails, roll back to the original state
      set({ files: originalFiles });
      // Here you could also show an error notification to the user
    } finally {
      // 5. Remove the file ID from the loading set to hide the spinner
      set((state) => {
        const newLoadingSet = new Set(state.fileActionLoading);
        newLoadingSet.delete(fileId);
        return { fileActionLoading: newLoadingSet };
      });
    }
  },

// Inside useDriveStore
MoveFileToTrash: async (filesToTrash: DriveFile[]) => {
    // NOTE: This could also be converted to an optimistic update for a faster feel.
    set({ isLoading: true });
    try {
      const { token } = useAuthStore.getState();
      const fileIds = filesToTrash.map(file => file.id);

      await axios.delete(`${import.meta.env.VITE_PUBLIC_FILE_SERVICE}/api/MoveToRecycleBin`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        data: fileIds
      });

      set((state) => ({
        files: state.files.filter(file => !fileIds.includes(file.id)),
        selectedFiles: []
      }));
    } catch (error) {
      console.error("Failed to move files to trash: ", error);
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

      await axios.post(`${import.meta.env.VITE_PUBLIC_FILE_SERVICE}/api/RestoreFiles`, fileIds, {
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

      await axios.delete(`${import.meta.env.VITE_PUBLIC_FILE_SERVICE}/api/PermanentlyDeleteFiles`, {
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
      `${import.meta.env.VITE_PUBLIC_FILE_SERVICE}/api/DownloadFiles`,
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

        let url =  `${import.meta.env.VITE_PUBLIC_SEARCH_SERVICE}/api/metadata/user/search`;
        let params = {};

        // If a query is provided, use the search endpoint
        if (query.trim() !== '') {
          url =  `${import.meta.env.VITE_PUBLIC_SEARCH_SERVICE}/api/metadata/search`;
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
      let url = `${import.meta.env.VITE_PUBLIC_SEARCH_SERVICE}/api/metadata/user/trash`;
      let params = {};

      // If a query is provided, use the search endpoint
      if (query.trim() !== '') {
        url = `${import.meta.env.VITE_PUBLIC_SEARCH_SERVICE}/api/metadata/search/trash`;
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
      const res = await axios.get<User>(`${import.meta.env.VITE_PUBLIC_AUTH_SERVICE}/api/auth/user`, {
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