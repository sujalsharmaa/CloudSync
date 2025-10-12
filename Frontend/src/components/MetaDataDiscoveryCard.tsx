import React, { useState, useEffect, useCallback, useMemo } from 'react';
import axios from 'axios';
import { Tag, Blocks, Loader2 } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
// import { useDriveStore } from '@/stores/driveStore'; // Removed unused store import

// --- Type Definitions ---
interface MetadataResponse {
    tags: string[];
    categories: string[];
}

interface MetadataDiscoveryCardProps {
    onSearch: (query: string) => void;
}

// --- Component Definition ---
export const MetadataDiscoveryCard = React.memo(function MetadataDiscoveryCard({ onSearch }: MetadataDiscoveryCardProps) {
    const [metadata, setMetadata] = useState<MetadataResponse | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const API_URL = 'http://localhost:8085/api/metadata/user/tagsAndCategories';
    const token = localStorage.getItem("auth_token") || null; 

    // Helper function to get a random sample of N items (stable due to empty dependency array)
    const getRandomSample = useCallback((arr: string[], count: number): string[] => {
        if (!arr || arr.length === 0) return [];
        
        // Shuffle array and take the first 'count' elements
        const shuffled = [...arr].sort(() => 0.5 - Math.random());
        return shuffled.slice(0, count);
    }, []); 

    // Fetch metadata only once on mount
    useEffect(() => {
        if (!token) {
            setError("Authentication token missing.");
            setLoading(false);
            return;
        }

        const fetchMetadata = async () => {
            try {
                const response = await axios.get<MetadataResponse>(API_URL, {
                    headers: { Authorization: `Bearer ${token}` },
                });
                setMetadata(response.data);
            } catch (err) {
                console.error("Failed to fetch metadata:", err);
                setError("Failed to load metadata. Please check the backend service.");
            } finally {
                setLoading(false);
            }
        };

        fetchMetadata();
    }, []); 
    
    // --- Memoize Random Tags and Categories ---

    // FIX 1: Change tag count to 12
    const randomTags = useMemo(() => {
        return metadata ? getRandomSample(metadata.tags,20) : [];
    }, [metadata, getRandomSample]);

    // FIX 2: Change category count to 12
    const randomCategories = useMemo(() => {
        return metadata ? getRandomSample(metadata.categories,12) : [];
    }, [metadata, getRandomSample]);

    // --- Loading State Renderer (omitted for brevity) ---
    const renderLoadingState = () => (
        <div className="space-y-4">
            <div className="flex flex-wrap gap-2">
                {[...Array(12)].map((_, i) => (
                    <div key={`tag-load-${i}`} className="h-5 w-20 animate-pulse rounded-full bg-gray-200 dark:bg-gray-700" />
                ))}
            </div>
            <Separator className="bg-gray-100 dark:bg-gray-800" />
            <div className="flex flex-wrap gap-2">
                {[...Array(12)].map((_, i) => (
                    <div key={`cat-load-${i}`} className="h-6 w-32 animate-pulse rounded-md bg-gray-200 dark:bg-gray-700" />
                ))}
            </div>
        </div>
    );

    // --- Error State Renderer (omitted for brevity) ---
    if (error) {
        return (
            <Card className="border-red-500 bg-red-50 dark:bg-red-950">
                <CardHeader><CardTitle className="text-lg text-red-600 dark:text-red-400">Data Error</CardTitle></CardHeader>
                <CardContent><p className="text-sm text-red-500 dark:text-red-300">{error}</p></CardContent>
            </Card>
        );
    }

    return (
        <Card className="shadow-lg transition-all hover:shadow-xl dark:border-gray-700 z-10 relative">
            <CardHeader className="p-4 border-b dark:border-gray-700">
                <CardTitle className="text-xl font-bold flex items-center text-primary dark:text-blue-400">
                    <Loader2 className={`w-5 h-5 mr-2 ${loading ? 'animate-spin' : 'hidden'}`} />
                    Content Discovery ✨
                </CardTitle>
            </CardHeader>
            <CardContent className="pt-4 p-4">
                {loading ? renderLoadingState() : (
                    <div className="space-y-6">
                        {/* Tags Section */}
                        <div>
                            {/* FIX 3: Update heading text to reflect '12' */}
                            <h3 className="text-sm font-semibold mb-3 text-gray-600 dark:text-gray-400 flex items-center">
                                <Tag className="w-4 h-4 mr-2 text-drive-blue" />
                                Trending Tags (Top {randomTags.length})
                            </h3>
                            <div className="flex flex-wrap gap-2">
                                {randomTags.map((tag, index) => (
                                    <Badge 
                                        key={index} 
                                        variant="outline"
                                        className="text-xs px-3 py-1 bg-blue-50 hover:bg-blue-100 dark:bg-gray-800 dark:hover:bg-gray-700 transition-colors cursor-pointer border-blue-200 dark:border-blue-900"
                                        onClick={() => onSearch(tag)} 
                                    >
                                        #{tag}
                                    </Badge>
                                ))}

                                {randomTags.length === 0 && <p className="text-sm text-muted-foreground italic">No tags found.</p>}
                            </div>
                        </div>

                        <Separator className="bg-gray-200 dark:bg-gray-700" />

                        {/* Categories Section */}
                        <div>
                             {/* FIX 3: Update heading text to reflect '12' */}
                            <h3 className="text-sm font-semibold mb-3 text-gray-600 dark:text-gray-400 flex items-center">
                                <Blocks className="w-4 h-4 mr-2 text-purple-600" />
                                Popular Categories (Top {randomCategories.length})
                            </h3>
                            <div className="flex flex-wrap gap-3">
                                {randomCategories.map((category, index) => (
                                    <Badge 
                                        key={index} 
                                        className="text-sm px-3 py-1 bg-purple-100 text-purple-700 hover:bg-purple-200 dark:bg-purple-900 dark:text-purple-300 dark:hover:bg-purple-800 transition-colors cursor-pointer font-medium"
                                        onClick={() => onSearch(category)} 
                                    >
                                        {category}
                                    </Badge>
                                ))}
                                {randomCategories.length === 0 && <p className="text-sm text-muted-foreground italic">No categories found.</p>}
                            </div>
                        </div>
                    </div>
                )}
            </CardContent>
        </Card>
    );
})