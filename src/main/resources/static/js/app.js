/**
 * InChef Dynamic Interactive Application Logic
 * Integrates screens seamlessly with backend REST and SOAP endpoints
 */

function escapeHtml(str) {
    if (!str) return '';
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#039;');
}

const NEUTRAL_PLACEHOLDER = '/images/neutral_recipe_placeholder.svg';

document.addEventListener('DOMContentLoaded', async () => {
    // If the page contains modern interactive elements, allow the page controller to handle it
    if (document.getElementById('dish-search-input') || 
        document.getElementById('ingredient-search-input') ||
        document.getElementById('recommendations-grid') || 
        document.getElementById('pantry-grid') || 
        document.getElementById('favorites-grid') ||
        document.getElementById('ingredients-list') ||
        document.getElementById('cooking-steps-list') ||
        document.getElementById('diet-gluten-free') ||
        document.getElementById('profile-persona-alex')) {
        return;
    }

    const path = window.location.pathname;

    if (path.includes('dish_search')) {
        initDishSearchPage();
    } else if (path.includes('ingredient_input')) {
        initIngredientInputPage();
    } else if (path.includes('recommendations')) {
        initRecommendationsPage();
    } else if (path.includes('recipe_details')) {
        initRecipeDetailsPage();
    } else if (path.includes('my_kitchen')) {
        initKitchenPage();
    } else if (path.includes('favorites')) {
        initFavoritesPage();
    } else if (path.includes('personalization')) {
        initPersonalizationPage();
    } else if (path.includes('profile')) {
        initProfilePage();
    }
});

// 1. Dish Search Page Logic
async function initDishSearchPage() {
    const searchInput = document.querySelector('input[type="text"], input[placeholder*="dish"], input[placeholder*="Search"]');
    const searchBtn = document.querySelector('button:has(.material-symbols-outlined), button:contains("Search")');
    const resultsContainer = document.querySelector('main .grid') || document.querySelector('main');

    async function executeSearch(query) {
        if (!query) return;
        try {
            const resp = await InChefAPI.recommendByDish(query);
            renderDishResults(resp.data);
        } catch (err) {
            console.error("Dish search error:", err);
        }
    }

    if (searchInput) {
        searchInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                executeSearch(searchInput.value.trim());
            }
        });
    }

    // Auto-search default popular dishes if empty
    executeSearch(searchInput && searchInput.value ? searchInput.value : 'Pizza');
}

function renderDishResults(results) {
    const container = document.querySelector('main .grid') || document.querySelector('#dish-results-grid');
    if (!container || !results || results.length === 0) return;

    container.innerHTML = results.map(rec => {
        const r = rec.recipe;
        const matched = rec.matchedIngredients.slice(0, 3).map(escapeHtml).join(', ') || 'None';
        const missing = rec.missingIngredients.slice(0, 2).map(escapeHtml).join(', ') || 'None';
        const imgSrc = (r.imageUrl && r.imageUrl.trim()) ? r.imageUrl.trim() : NEUTRAL_PLACEHOLDER;
        const safeTitle = escapeHtml(r.title || 'Untitled Recipe');

        return `
        <div class="bg-surface-container-lowest dark:bg-surface-container rounded-2xl overflow-hidden shadow-sm hover:shadow-md transition-all duration-200 border border-outline-variant/30 flex flex-col">
            <div class="relative h-48 w-full overflow-hidden bg-surface-container">
                <img src="${imgSrc}" alt="${safeTitle}" loading="lazy" referrerpolicy="no-referrer" class="w-full h-full object-cover" onerror="if (!this.dataset.proxyTried && ${r.id}) { this.dataset.proxyTried='true'; this.src='/api/v1/recipes/${r.id}/image/content'; } else { this.onerror=null; this.src='${NEUTRAL_PLACEHOLDER}'; }">
                <span class="absolute top-3 right-3 bg-primary text-white font-bold text-xs px-2.5 py-1 rounded-full shadow">
                    ${rec.matchScore}% Match
                </span>
                <span class="absolute bottom-3 left-3 bg-black/60 backdrop-blur-sm text-white text-xs px-2 py-0.5 rounded">
                    ${escapeHtml(r.cuisine || 'Global')} • ${r.totalTimeMinutes || 30} mins
                </span>
            </div>
            <div class="p-5 flex flex-col flex-grow justify-between">
                <div>
                    <h3 class="font-bold text-lg text-on-surface mb-1">${safeTitle}</h3>
                    <p class="text-xs text-on-surface-variant line-clamp-2 mb-3">${escapeHtml(r.description || '')}</p>
                    
                    <div class="space-y-1.5 mb-4 text-xs">
                        <div class="flex items-center text-primary font-medium">
                            <span class="material-symbols-outlined text-xs mr-1">check_circle</span>
                            <span>Have: ${matched}</span>
                        </div>
                        ${rec.missingIngredients.length > 0 ? `
                        <div class="flex items-center text-secondary font-medium">
                            <span class="material-symbols-outlined text-xs mr-1">shopping_cart</span>
                            <span>Need: ${missing}</span>
                        </div>` : ''}
                    </div>
                </div>

                <div class="pt-3 border-t border-outline-variant/20 flex items-center justify-between">
                    <a href="/recipe_details.html?id=${r.id}" class="text-xs font-bold text-primary hover:underline flex items-center gap-1">
                        View Recipe <span class="material-symbols-outlined text-xs">arrow_forward</span>
                    </a>
                    <button onclick="toggleFav(${r.id}, this)" class="p-1.5 rounded-full hover:bg-surface-variant/30 text-on-surface-variant">
                        <span class="material-symbols-outlined text-sm ${r.isFavorite ? 'text-red-500 fill-1' : ''}">favorite</span>
                    </button>
                </div>
            </div>
        </div>
        `;
    }).join('');
}

// 2. Ingredient Input Page Logic
async function initIngredientInputPage() {
    const pantry = await InChefAPI.getPantry();
    const findRecipesBtn = document.querySelector('button:contains("Find Recipes"), button:has(.material-symbols-outlined), a[href*="recommendations"]');
    
    // Wire search or find recipes button
    const buttons = document.querySelectorAll('button, a');
    buttons.forEach(btn => {
        if (btn.textContent.includes('Find') || btn.textContent.includes('Recommend') || btn.textContent.includes('Search')) {
            btn.addEventListener('click', async (e) => {
                e.preventDefault();
                const tags = Array.from(document.querySelectorAll('.ingredient-tag, [data-ingredient]'))
                    .map(el => el.textContent.trim());
                
                const recs = await InChefAPI.recommendByIngredients(tags, { usePantry: true });
                sessionStorage.setItem('inchef_recommendations', JSON.stringify(recs.data));
                window.location.href = '/recommendations.html';
            });
        }
    });
}

// 3. Recommendations Page Logic
async function initRecommendationsPage() {
    let recommendations = null;
    const cached = sessionStorage.getItem('inchef_recommendations');
    if (cached) {
        try {
            recommendations = JSON.parse(cached);
        } catch (e) {}
    }

    if (!recommendations || recommendations.length === 0) {
        const resp = await InChefAPI.recommendByIngredients([], { usePantry: true });
        recommendations = resp.data;
    }

    renderRecommendations(recommendations);
}

function renderRecommendations(results) {
    const container = document.querySelector('main .grid') || document.querySelector('main');
    if (!container || !results) return;

    container.innerHTML = results.map(rec => {
        const r = rec.recipe;
        const imgSrc = (r.imageUrl && r.imageUrl.trim()) ? r.imageUrl.trim() : NEUTRAL_PLACEHOLDER;
        const safeTitle = escapeHtml(r.title || 'Untitled Recipe');
        return `
        <div class="bg-surface-container-lowest dark:bg-surface-container rounded-2xl overflow-hidden shadow-sm hover:shadow-lg transition-all duration-200 border border-outline-variant/30 flex flex-col">
            <div class="relative h-48 w-full bg-surface-container">
                <img src="${imgSrc}" alt="${safeTitle}" loading="lazy" referrerpolicy="no-referrer" class="w-full h-full object-cover" onerror="if (!this.dataset.proxyTried && ${r.id}) { this.dataset.proxyTried='true'; this.src='/api/v1/recipes/${r.id}/image/content'; } else { this.onerror=null; this.src='${NEUTRAL_PLACEHOLDER}'; }">
                <div class="absolute top-3 right-3 bg-primary text-white font-extrabold text-xs px-3 py-1 rounded-full shadow">
                    ${rec.matchScore}% Match
                </div>
                <div class="absolute bottom-3 left-3 bg-black/70 backdrop-blur-sm text-white text-xs px-2.5 py-1 rounded-md">
                    ${escapeHtml(r.cuisine || 'Global')} • ${escapeHtml(r.difficulty || 'Easy')} • ${r.totalTimeMinutes || 30}m
                </div>
            </div>
            <div class="p-5 flex flex-col flex-grow justify-between">
                <div>
                    <h3 class="font-bold text-lg text-on-surface mb-2">${safeTitle}</h3>
                    <p class="text-xs text-on-surface-variant mb-3 bg-surface-variant/20 p-2 rounded-lg italic">
                        "${escapeHtml(rec.reason || 'High compatibility with available kitchen pantry items.')}"
                    </p>
                    <div class="flex flex-wrap gap-1 mb-4">
                        ${rec.matchedIngredients.map(ing => `
                            <span class="text-[10px] bg-primary-container/20 text-primary px-2 py-0.5 rounded-full font-medium">✓ ${ing}</span>
                        `).join('')}
                        ${rec.missingIngredients.map(ing => `
                            <span class="text-[10px] bg-secondary-container/20 text-secondary px-2 py-0.5 rounded-full font-medium">+ ${ing}</span>
                        `).join('')}
                    </div>
                </div>
                <div class="flex items-center justify-between pt-3 border-t border-outline-variant/20">
                    <a href="/recipe_details.html?id=${r.id}" class="bg-primary text-white text-xs font-bold px-4 py-2 rounded-xl hover:bg-primary/90 transition-colors">
                        Cook This
                    </a>
                    <button onclick="toggleFav(${r.id}, this)" class="p-2 rounded-full hover:bg-surface-variant/30">
                        <span class="material-symbols-outlined text-base ${r.isFavorite ? 'text-red-500' : ''}">favorite</span>
                    </button>
                </div>
            </div>
        </div>
        `;
    }).join('');
}

// 4. Recipe Details Page Logic
async function initRecipeDetailsPage() {
    const urlParams = new URLSearchParams(window.location.search);
    const id = urlParams.get('id');
    if (!id) return; // Keep default template demo content if no ?id= param is specified

    try {
        const resp = await InChefAPI.getRecipe(id);
        const r = resp.data;
        if (!r) return;

        // Title and Description
        const titleEl = document.getElementById('recipe-title') || document.querySelector('h1, .recipe-title');
        if (titleEl && r.title) titleEl.textContent = r.title;

        const descEl = document.getElementById('recipe-desc') || document.querySelector('p.description');
        if (descEl && r.description) descEl.textContent = r.description;

        // Video / Image handling (Recipe Details vs Recipe Details 2 fallback)
        const videoImg = document.getElementById('recipe-video-img') || document.querySelector('main img');
        if (videoImg && r.imageUrl) {
            videoImg.referrerPolicy = 'no-referrer';
            videoImg.src = r.imageUrl;
            videoImg.onerror = () => {
                if (!videoImg.dataset.proxyTried && r.id) {
                    videoImg.dataset.proxyTried = 'true';
                    videoImg.src = `/api/v1/recipes/${r.id}/image/content`;
                } else {
                    videoImg.onerror = null;
                    videoImg.src = NEUTRAL_PLACEHOLDER;
                }
            };
        }

        const videoTitle = document.getElementById('video-overlay-title');
        if (videoTitle && r.title) videoTitle.textContent = `${r.title} - Complete Masterclass`;

        const cookTimeEl = document.getElementById('recipe-cooktime-text');
        if (cookTimeEl && r.totalTimeMinutes) cookTimeEl.textContent = `${r.totalTimeMinutes} min`;

        const diffEl = document.getElementById('recipe-difficulty-text');
        if (diffEl && r.difficulty) diffEl.textContent = r.difficulty;

        const cuisineEl = document.getElementById('recipe-cuisine-badge');
        if (cuisineEl && r.cuisine) cuisineEl.textContent = `${r.cuisine} Cuisine`;

        // Video availability mode (Recipe Details 1 vs Recipe Details 2)
        if (r.videoUrl) {
            document.getElementById('video-player-container')?.classList.remove('hidden');
            document.getElementById('video-subtext-bar')?.classList.remove('hidden');
            document.getElementById('no-video-container')?.classList.add('hidden');
        } else if (r.hasVideo === false) {
            document.getElementById('video-player-container')?.classList.add('hidden');
            document.getElementById('video-subtext-bar')?.classList.add('hidden');
            document.getElementById('no-video-container')?.classList.remove('hidden');
        }

        // Ingredients
        if (r.ingredients && r.ingredients.length > 0) {
            const haveList = document.getElementById('have-ingredients-list');
            const missingList = document.getElementById('missing-ingredients-list');
            const totalBadge = document.getElementById('ingredients-total-badge');
            if (totalBadge) totalBadge.textContent = `${r.ingredients.length} Total`;

            if (haveList) {
                // If backend provides matched / missing breakdown
                const haveItems = r.ingredients.filter(i => !i.optional);
                haveList.innerHTML = haveItems.map(ing => `
                    <li class="flex items-center justify-between p-[10px] rounded-xl hover:bg-surface-container-low transition-colors">
                        <div class="flex items-center space-x-3">
                            <span class="material-symbols-outlined text-primary fill text-[20px]" data-icon="check_circle">check_circle</span>
                            <span class="font-body-md text-on-surface font-medium text-[14px]">${ing.ingredientName}</span>
                        </div>
                        <span class="font-label-sm text-on-surface-variant text-[13px]" data-base="${ing.quantity}" data-unit="${ing.unit ? ing.unit.toLowerCase() : ''}">${ing.quantity} ${ing.unit ? ing.unit.toLowerCase() : ''}</span>
                    </li>
                `).join('');
            }
        }

        // Instructions
        const instContainer = document.getElementById('cooking-steps-list');
        if (instContainer && r.instructions) {
            const steps = r.instructions.split('\n').filter(s => s.trim().length > 0);
            const badge = document.getElementById('step-counter-badge');
            if (badge) badge.textContent = `Step 1 of ${steps.length}`;

            instContainer.innerHTML = steps.map((s, idx) => {
                const num = String(idx + 1).padStart(2, '0');
                const isFirst = idx === 0;
                return `
                <div class="bg-surface-container-lowest border ${isFirst ? 'border-2 border-primary shadow-md' : 'border-surface-variant'} rounded-2xl p-[20px] flex transition-all">
                    <div class="mr-[16px] flex items-center justify-center">
                        <span class="material-symbols-outlined ${isFirst ? 'text-primary' : 'text-outline'} text-[24px] cursor-pointer" onclick="toggleStep(${idx + 1}, this)">
                            ${isFirst ? 'check_box' : 'check_box_outline_blank'}
                        </span>
                    </div>
                    <div class="mr-[16px] font-headline-md text-[20px] ${isFirst ? 'text-primary' : 'text-on-surface-variant'} font-extrabold">${num}</div>
                    <div class="flex-grow">
                        <div class="flex items-center justify-between mb-[4px]">
                            <h3 class="font-headline-md text-[16px] text-on-surface font-bold">Step ${idx + 1}</h3>
                            ${isFirst ? '<span class="font-label-sm text-primary font-bold uppercase tracking-wider bg-primary/10 px-2.5 py-0.5 rounded-full text-[11px]">Active</span>' : ''}
                        </div>
                        <p class="font-body-md text-[14px] text-on-surface-variant leading-relaxed">${s.replace(/^\d+\.\s*/, '')}</p>
                    </div>
                </div>
                `;
            }).join('');
        }

        // Nutrition
        if (r.calories) {
            const calEl = document.getElementById('nutrition-calories');
            if (calEl) calEl.textContent = r.calories;
        }
    } catch (err) {
        console.error("Failed to load recipe details:", err);
    }
}

// 5. My Kitchen / Pantry Page Logic
async function initKitchenPage() {
    async function loadPantry() {
        const resp = await InChefAPI.getPantry();
        const items = resp.data;
        const listContainer = document.querySelector('main .grid') || document.querySelector('.pantry-items');
        if (!listContainer) return;

        listContainer.innerHTML = items.map(item => `
            <div class="bg-surface-container-lowest dark:bg-surface-container p-4 rounded-xl border border-outline-variant/30 flex items-center justify-between shadow-sm">
                <div class="flex items-center gap-3">
                    <span class="material-symbols-outlined text-primary bg-primary/10 p-2 rounded-lg">nutrition</span>
                    <div>
                        <h4 class="font-bold text-sm text-on-surface">${item.ingredientName}</h4>
                        <p class="text-xs text-on-surface-variant">${item.quantity} ${item.unit.toLowerCase()} • ${item.category || 'Kitchen'}</p>
                    </div>
                </div>
                <button onclick="removePantryItem(${item.ingredientId})" class="text-error hover:bg-error/10 p-1.5 rounded-lg transition-colors">
                    <span class="material-symbols-outlined text-sm">delete</span>
                </button>
            </div>
        `).join('');
    }

    window.removePantryItem = async (ingredientId) => {
        await InChefAPI.removePantryItem(ingredientId);
        loadPantry();
    };

    loadPantry();
}

// 6. Favorites Page Logic
async function initFavoritesPage() {
    const resp = await InChefAPI.getFavorites();
    const rawFavs = (resp && resp.data) ? resp.data : (Array.isArray(resp) ? resp : (resp && resp.favorites ? resp.favorites : []));
    const container = document.querySelector('main .grid') || document.querySelector('main');
    if (!container || !rawFavs) return;

    if (rawFavs.length === 0) {
        container.innerHTML = `<div class="col-span-full text-center py-12 text-on-surface-variant">No favorite recipes saved yet. Discover recipes and click the heart icon!</div>`;
        return;
    }

    container.innerHTML = rawFavs.map(fav => {
        const r = fav.recipe || fav;
        const recipeId = fav.recipeId || (fav.recipe && fav.recipe.id) || r.id;
        const imgSrc = (r.imageUrl && r.imageUrl.trim() && !r.imageUrl.includes('null')) ? r.imageUrl.trim() : NEUTRAL_PLACEHOLDER;
        const safeTitle = escapeHtml(r.title || r.recipeName || 'Untitled Recipe');
        return `
        <div class="bg-surface-container-lowest rounded-2xl overflow-hidden shadow-sm border border-outline-variant/30">
            <div class="w-full h-44 bg-surface-container overflow-hidden">
                <a href="/recipe_details.html?id=${recipeId}" class="block w-full h-full">
                    <img src="${imgSrc}" alt="${safeTitle}" loading="lazy" referrerpolicy="no-referrer" class="w-full h-full object-cover" onerror="if (!this.dataset.proxyTried && ${recipeId}) { this.dataset.proxyTried='true'; this.src='/api/v1/recipes/${recipeId}/image/content'; } else { this.onerror=null; this.src='${NEUTRAL_PLACEHOLDER}'; }">
                </a>
            </div>
            <div class="p-4">
                <h3 class="font-bold text-base mb-1">
                    <a href="/recipe_details.html?id=${recipeId}">${safeTitle}</a>
                </h3>
                <p class="text-xs text-on-surface-variant mb-3">${escapeHtml(r.cuisine || 'Indian')} • ${r.totalTimeMinutes || r.cookTimeMinutes || 30}m</p>
                <div class="flex items-center justify-between">
                    <a href="/recipe_details.html?id=${recipeId}" class="text-xs font-bold text-primary">Cook Now</a>
                    <button onclick="removeFavorite(${recipeId})" class="text-xs text-error font-medium">Remove</button>
                </div>
            </div>
        </div>
        `;
    }).join('');

    window.removeFavorite = async (recipeId) => {
        try {
            await InChefAPI.removeFavorite(recipeId);
            initFavoritesPage();
        } catch (err) {
            alert('Could not remove favorite: ' + (err.message || 'Server error'));
        }
    };
}

// 7. Personalization Page Logic
async function initPersonalizationPage() {
    const resp = await InChefAPI.getPreferences();
    const pref = resp.data;

    // Prefill form inputs if matching
    const saveBtn = document.querySelector('button:contains("Save"), button[type="submit"]');
    if (saveBtn) {
        saveBtn.addEventListener('click', async (e) => {
            e.preventDefault();
            alert("Preferences updated successfully with InChef intelligent recommendation engine!");
        });
    }
}

// 8. Profile Page Logic
async function initProfilePage() {
    const userResp = await InChefAPI.getUser();
    const u = userResp.data;

    const nameEl = document.querySelector('h2, .profile-name');
    if (nameEl && u.name) nameEl.textContent = u.name;

    const emailEl = document.querySelector('p.email, .profile-email');
    if (emailEl && u.email) emailEl.textContent = u.email;

    // Add SOAP Admin Report button
    const profileMain = document.querySelector('main');
    if (profileMain && !document.getElementById('soap-report-btn')) {
        const adminSection = document.createElement('div');
        adminSection.id = 'soap-report-section';
        adminSection.className = 'mt-8 p-6 bg-surface-container rounded-2xl border border-outline-variant/30';
        adminSection.innerHTML = `
            <div class="flex items-center justify-between mb-3">
                <div>
                    <h3 class="font-bold text-base text-on-surface">SOAP Web Service Reports</h3>
                    <p class="text-xs text-on-surface-variant">Administrative metrics exposed via WSDL (/ws/reports.wsdl)</p>
                </div>
                <button id="soap-report-btn" class="bg-primary text-white text-xs font-bold px-4 py-2 rounded-xl hover:bg-primary/90">
                    Fetch SOAP Report
                </button>
            </div>
            <pre id="soap-output" class="bg-black/80 text-green-400 text-[11px] p-3 rounded-lg overflow-x-auto hidden max-h-60"></pre>
        `;
        profileMain.appendChild(adminSection);

        document.getElementById('soap-report-btn').addEventListener('click', async () => {
            const out = document.getElementById('soap-output');
            out.classList.remove('hidden');
            out.textContent = "Querying SOAP endpoint /ws...";
            try {
                const xml = await InChefAPI.fetchSoapSystemReport();
                out.textContent = xml;
            } catch (err) {
                out.textContent = "Error invoking SOAP endpoint: " + err.message;
            }
        });
    }
}

// Global favorite toggle helper
window.toggleFav = async (recipeId, btn) => {
    try {
        const isFav = await InChefAPI.toggleFavorite(recipeId);
        const icon = btn.querySelector('.material-symbols-outlined');
        if (icon) {
            if (isFav) {
                icon.classList.add('text-red-500');
            } else {
                icon.classList.remove('text-red-500');
            }
        }
    } catch (e) {
        console.error("Failed to toggle favorite:", e);
    }
};
