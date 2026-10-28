/**
 * InChef Frontend API Client
 * Seamlessly connects HTML screens to Spring Boot REST and SOAP APIs
 */
const InChefAPI = {
    baseUrl: '/api/v1',
    currentUserId: localStorage.getItem('inchef_userId') ? parseInt(localStorage.getItem('inchef_userId')) : null,

    setCurrentUser(userId) {
        this.currentUserId = userId ? parseInt(userId) : null;
        if (this.currentUserId) {
            localStorage.setItem('inchef_userId', this.currentUserId);
        } else {
            localStorage.removeItem('inchef_userId');
        }
    },

    getCurrentUserId() {
        return this.currentUserId;
    },

    isAuthenticated() {
        return !!this.currentUserId;
    },

    getStoredUser() {
        try {
            const raw = localStorage.getItem('inchef_user');
            return raw ? JSON.parse(raw) : null;
        } catch {
            return null;
        }
    },

    async request(path, options = {}) {
        const url = `${this.baseUrl}${path}`;
        const defaultHeaders = {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        };

        const config = {
            credentials: 'include',
            ...options,
            headers: {
                ...defaultHeaders,
                ...options.headers
            }
        };

        try {
            const response = await fetch(url, config);
            let data = null;
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                data = await response.json();
            }
            if (!response.ok) {
                const errMsg = (data && (data.message || data.error)) || `Request failed with status ${response.status}`;
                const error = new Error(errMsg);
                error.status = response.status;
                error.data = data;
                throw error;
            }
            return data;
        } catch (err) {
            console.error(`[InChefAPI Error] ${url}:`, err);
            throw err;
        }
    },

    // Auth
    async login(email, password) {
        const res = await this.request('/auth/login', {
            method: 'POST',
            body: JSON.stringify({ email, password })
        });
        if (res && res.data && res.data.user) {
            this.setCurrentUser(res.data.user.id);
            localStorage.setItem('inchef_user', JSON.stringify(res.data.user));
        }
        return res;
    },

    async register(userData) {
        const res = await this.request('/auth/register', {
            method: 'POST',
            body: JSON.stringify(userData)
        });
        if (res && res.data && res.data.user) {
            this.setCurrentUser(res.data.user.id);
            localStorage.setItem('inchef_user', JSON.stringify(res.data.user));
        }
        return res;
    },

    async getGoogleConfig() {
        return this.request('/auth/google/config');
    },

    async loginWithGoogle(idToken) {
        const res = await this.request('/auth/google', {
            method: 'POST',
            body: JSON.stringify({ idToken })
        });
        if (res && res.data && res.data.user) {
            this.setCurrentUser(res.data.user.id);
            localStorage.setItem('inchef_user', JSON.stringify(res.data.user));
        }
        return res;
    },

    async logout() {
        try {
            await this.request('/auth/logout', { method: 'POST' });
        } catch (e) {
            console.warn('Logout error', e);
        }
        localStorage.removeItem('inchef_user');
        localStorage.removeItem('inchef_userId');
        this.currentUserId = null;
        window.location.href = '/auth.html?mode=login';
    },

    async getMe() {
        return this.request('/auth/me');
    },

    // Users
    async getUsers() {
        return this.request('/users');
    },

    async getUser(id = this.currentUserId) {
        const uid = id || this.currentUserId;
        if (!uid) throw new Error('Authentication required');
        return this.request(`/users/${uid}`);
    },

    async updateUser(id, data) {
        const uid = id || this.currentUserId;
        if (!uid) throw new Error('Authentication required');
        return this.request(`/users/${uid}`, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
    },

    // Ingredients
    async getIngredients(query = '', category = '', limit = null) {
        const params = new URLSearchParams();
        if (query) params.append('query', query);
        if (category) params.append('category', category);
        if (limit) params.append('limit', limit);
        return this.request(`/ingredients?${params.toString()}`);
    },

    async autocompleteIngredients(query = '', limit = 10) {
        if (!query || !query.trim()) return { data: [] };
        const params = new URLSearchParams({ query: query.trim(), limit });
        return this.request(`/ingredients/autocomplete?${params.toString()}`);
    },

    // Recipes
    async getCuisines() {
        return this.request('/recipes/cuisines');
    },

    async getRecipes(params = {}) {
        const queryParams = new URLSearchParams();
        if (params.query && params.query.trim().toLowerCase() !== 'all') queryParams.append('query', params.query.trim());
        if (params.cuisine && params.cuisine.trim().toLowerCase() !== 'all') queryParams.append('cuisine', params.cuisine.trim());
        if (params.dietaryType && params.dietaryType.trim().toLowerCase() !== 'all') queryParams.append('dietaryType', params.dietaryType.trim());
        if (params.mealType && params.mealType.trim().toLowerCase() !== 'all') queryParams.append('mealType', params.mealType.trim());
        if (params.difficulty && params.difficulty.trim().toLowerCase() !== 'all') queryParams.append('difficulty', params.difficulty.trim());
        if (params.maxTime) queryParams.append('maxTime', params.maxTime);
        if (this.currentUserId) queryParams.append('userId', this.currentUserId);
        if (params.page !== undefined) queryParams.append('page', params.page);
        if (params.size !== undefined) queryParams.append('size', params.size);

        return this.request(`/recipes?${queryParams.toString()}`);
    },

    async searchRecipes(query = '', options = {}) {
        const queryParams = new URLSearchParams();
        const cleanQuery = query && query.trim().toLowerCase() !== 'all' ? query.trim() : '';
        if (cleanQuery) queryParams.append('query', cleanQuery);
        if (options.cuisine && options.cuisine.trim().toLowerCase() !== 'all') queryParams.append('cuisine', options.cuisine.trim());
        if (options.dietaryType && options.dietaryType.trim().toLowerCase() !== 'all') queryParams.append('dietaryType', options.dietaryType.trim());
        if (options.mealType && options.mealType.trim().toLowerCase() !== 'all') queryParams.append('mealType', options.mealType.trim());
        if (options.difficulty && options.difficulty.trim().toLowerCase() !== 'all') queryParams.append('difficulty', options.difficulty.trim());
        if (options.maxTime) queryParams.append('maxTime', options.maxTime);
        if (options.sort) queryParams.append('sort', options.sort);
        if (this.currentUserId) queryParams.append('userId', this.currentUserId);
        queryParams.append('size', options.size || 50);
        if (options.page !== undefined) queryParams.append('page', options.page);

        return this.request(`/recipes/search?${queryParams.toString()}`);
    },

    async getRecipe(id) {
        const q = this.currentUserId ? `?userId=${this.currentUserId}` : '';
        return this.request(`/recipes/${id}${q}`);
    },

    async getInstructionSteps(recipeId) {
        return this.request(`/recipes/${recipeId}/instructions`);
    },

    async getCookingProgress(recipeId, totalSteps = 0) {
        const userParam = this.currentUserId ? `&userId=${this.currentUserId}` : '';
        return this.request(`/recipes/${recipeId}/cooking-progress?totalSteps=${totalSteps}${userParam}`);
    },

    async updateCookingProgress(recipeId, progressData) {
        return this.request(`/recipes/${recipeId}/cooking-progress`, {
            method: 'POST',
            body: JSON.stringify({
                ...(this.currentUserId ? { userId: this.currentUserId } : {}),
                ...progressData
            })
        });
    },

    async resetCookingProgress(recipeId) {
        const userParam = this.currentUserId ? `?userId=${this.currentUserId}` : '';
        return this.request(`/recipes/${recipeId}/cooking-progress/reset${userParam}`, {
            method: 'POST'
        });
    },

    // Recommendations
    async recommendByIngredients(ingredients = [], options = {}) {
        return this.request('/recommendations/by-ingredients', {
            method: 'POST',
            body: JSON.stringify({
                userId: this.currentUserId || null,
                ingredients: ingredients,
                usePantry: options.usePantry !== undefined ? options.usePantry : true,
                dietaryType: options.dietaryType || null,
                cuisine: options.cuisine || null,
                mealType: options.mealType || null,
                maxCookingTime: options.maxCookingTime || null,
                limit: options.limit || 12
            })
        });
    },

    async recommendByDish(dishName, options = {}) {
        return this.request('/recommendations/by-dish', {
            method: 'POST',
            body: JSON.stringify({
                userId: this.currentUserId || null,
                dishName: dishName,
                userIngredients: options.userIngredients || [],
                usePantry: options.usePantry !== undefined ? options.usePantry : true,
                limit: options.limit || 12
            })
        });
    },

    // Kitchen / Pantry
    async getPantry(userId = this.currentUserId) {
        const uid = userId || this.currentUserId;
        if (!uid) throw new Error('Authentication required');
        return this.request(`/users/${uid}/pantry`);
    },

    async addPantryItem(item, userId = this.currentUserId) {
        const uid = userId || this.currentUserId;
        if (!uid) throw new Error('Authentication required');

        const payload = { ...item };
        if (payload.unit && typeof payload.unit === 'string' && payload.unit.toLowerCase() === 'item') {
            payload.unit = 'PIECES';
        } else if (!payload.unit) {
            payload.unit = 'PIECES';
        }
        if (!payload.quantity || payload.quantity <= 0) {
            payload.quantity = 1.0;
        }

        return this.request(`/users/${uid}/pantry`, {
            method: 'POST',
            body: JSON.stringify(payload)
        });
    },

    async removePantryItem(ingredientId, userId = this.currentUserId) {
        const uid = userId || this.currentUserId;
        if (!uid) throw new Error('Authentication required');
        return this.request(`/users/${uid}/pantry/${ingredientId}`, {
            method: 'DELETE'
        });
    },

    async clearPantry(userId = this.currentUserId) {
        const uid = userId || this.currentUserId;
        if (!uid) throw new Error('Authentication required');
        return this.request(`/users/${uid}/pantry`, {
            method: 'DELETE'
        });
    },

    // Preferences
    async getPreferences(userId = this.currentUserId) {
        const uid = userId || this.currentUserId;
        if (!uid) throw new Error('Authentication required');
        return this.request(`/users/${uid}/preferences`);
    },

    async updatePreferences(preferences, userId = this.currentUserId) {
        const uid = userId || this.currentUserId;
        if (!uid) throw new Error('Authentication required');
        const payload = { ...preferences };
        if (payload.preferredCuisines !== undefined && !Array.isArray(payload.preferredCuisines)) {
            if (typeof payload.preferredCuisines === 'string') {
                payload.preferredCuisines = payload.preferredCuisines.trim()
                    ? payload.preferredCuisines.split(',').map(s => s.trim()).filter(Boolean)
                    : [];
            } else if (payload.preferredCuisines === null) {
                payload.preferredCuisines = [];
            }
        }
        return this.request(`/users/${uid}/preferences`, {
            method: 'PUT',
            body: JSON.stringify(payload)
        });
    },

    // Login History
    async getLoginHistory(page = 0, size = 10, userId = null) {
        const path = userId
            ? `/users/${userId}/login-history?page=${page}&size=${size}`
            : `/users/me/login-history?page=${page}&size=${size}`;
        return this.request(path);
    },

    // Favorites
    async getFavorites(userId = this.currentUserId) {
        const uid = userId || this.currentUserId;
        return uid ? this.request(`/users/${uid}/favorites`) : this.request('/favorites');
    },

    async toggleFavorite(recipeId, userId = this.currentUserId) {
        const uid = userId || this.currentUserId;
        const checkPath = uid ? `/users/${uid}/favorites/${recipeId}/check` : `/favorites/${recipeId}/check`;
        const actionPath = uid ? `/users/${uid}/favorites/${recipeId}` : `/favorites/${recipeId}`;

        const check = await this.request(checkPath);
        if (check && check.data && check.data.isFavorite) {
            await this.request(actionPath, { method: 'DELETE' });
            return false;
        } else {
            await this.request(actionPath, { method: 'POST' });
            return true;
        }
    },

    async addFavorite(recipeId, userId = this.currentUserId) {
        const uid = userId || this.currentUserId;
        const path = uid ? `/users/${uid}/favorites/${recipeId}` : `/favorites/${recipeId}`;
        return this.request(path, { method: 'POST' });
    },

    async removeFavorite(recipeOrFavoriteId, userId = this.currentUserId) {
        const uid = userId || this.currentUserId;
        const path = uid ? `/users/${uid}/favorites/${recipeOrFavoriteId}` : `/favorites/${recipeOrFavoriteId}`;
        return this.request(path, { method: 'DELETE' });
    },

    async checkFavorite(recipeId, userId = this.currentUserId) {
        const uid = userId || this.currentUserId;
        const path = uid ? `/users/${uid}/favorites/${recipeId}/check` : `/favorites/${recipeId}/check`;
        return this.request(path);
    },

    async getFavoriteCount(userId = this.currentUserId) {
        const uid = userId || this.currentUserId;
        const path = uid ? `/users/${uid}/favorites/count` : `/favorites/count`;
        return this.request(path);
    },

    // SOAP Administrative Reports
    async fetchSoapSystemReport() {
        const soapEnvelope = `<?xml version="1.0" encoding="utf-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:rep="http://inchef.com/soap/reports">
   <soapenv:Header/>
   <soapenv:Body>
      <rep:GetSystemReportRequest/>
   </soapenv:Body>
</soapenv:Envelope>`;

        const response = await fetch('/ws', {
            method: 'POST',
            headers: {
                'Content-Type': 'text/xml;charset=UTF-8',
                'SOAPAction': ''
            },
            body: soapEnvelope
        });

        const xmlText = await response.text();
        return xmlText;
    }
};

window.InChefAPI = InChefAPI;
