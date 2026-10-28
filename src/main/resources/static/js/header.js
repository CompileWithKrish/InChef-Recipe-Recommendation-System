/**
 * InChef Global Reusable Header & Footer Component
 * Standardized responsive header component across Home, Discover, Recommendations, My Kitchen, Favorites, Recipe Details, and Profile.
 */
(function () {
    'use strict';

    function getActiveRoute() {
        const headerEl = document.getElementById('global-header');
        if (headerEl && headerEl.getAttribute('data-active')) {
            return headerEl.getAttribute('data-active').toLowerCase();
        }

        const path = window.location.pathname.toLowerCase();
        if (path.includes('discover') || path.includes('dish_search')) {
            return 'discover';
        } else if (path.includes('my_kitchen') || path.includes('ingredient_input')) {
            return 'kitchen';
        } else if (path.includes('favorites')) {
            return 'favorites';
        } else if (path.includes('recommendations')) {
            return 'recommendations';
        } else if (path.includes('profile') || path.includes('personalization')) {
            return 'profile';
        } else if (path.includes('recipe_details')) {
            return 'details';
        } else if (path.includes('auth')) {
            return 'auth';
        }
        return 'home';
    }

    async function checkAuthState() {
        if (!window.InChefAPI) return null;
        let user = InChefAPI.getStoredUser();
        try {
            const meRes = await InChefAPI.getMe();
            if (meRes && meRes.data) {
                user = meRes.data;
                InChefAPI.setCurrentUser(user.id);
                localStorage.setItem('inchef_user', JSON.stringify(user));
            }
        } catch (e) {
            // Not authenticated or session expired
            if (!user) {
                localStorage.removeItem('inchef_user');
            }
        }
        return user;
    }

    async function renderHeader() {
        const headerEl = document.getElementById('global-header');
        if (!headerEl) return;

        const active = getActiveRoute();
        const user = await checkAuthState();

        const navLinks = [
            { id: 'home', label: 'Home', href: '/index.html' },
            { id: 'discover', label: 'Discover', href: '/discover.html' },
            { id: 'recommendations', label: 'Recommendations', href: '/recommendations.html' },
            { id: 'kitchen', label: 'My Kitchen', href: '/my_kitchen.html' },
            { id: 'favorites', label: 'Favorites', href: '/favorites.html' }
        ];

        let authSection = '';
        if (user && user.email) {
            const initial = (user.name || user.email || 'U').charAt(0).toUpperCase();
            authSection = `
                <div class="flex items-center gap-3">
                    <a href="/profile.html" class="flex items-center gap-2 p-1 pl-1 pr-3 rounded-full bg-white border border-[#004538]/15 hover:border-[#004538]/40 shadow-xs transition-all ${active === 'profile' ? 'ring-2 ring-primary' : ''}" title="My Profile">
                        <div class="w-8 h-8 rounded-full bg-primary text-white font-bold flex items-center justify-center text-xs shadow-xs">
                            ${initial}
                        </div>
                        <span class="text-xs font-bold text-[#004538] max-w-[100px] truncate hidden sm:inline">${user.name || user.email.split('@')[0]}</span>
                    </a>
                </div>
            `;
        } else {
            authSection = `
                <div class="flex items-center gap-2.5">
                    <a href="/auth.html?mode=login" class="text-sm font-semibold text-on-surface hover:text-primary px-3 py-2 transition-colors flex items-center gap-1">
                        <span class="material-symbols-outlined text-[18px]">person</span>
                        <span>Login</span>
                    </a>
                    <a href="/auth.html?mode=signup" class="text-sm font-bold text-white bg-primary hover:bg-[#0f3d32] transition-all px-5 py-2 rounded-full shadow-xs hover:shadow">
                        Sign Up
                    </a>
                </div>
            `;
        }

        headerEl.className = 'bg-[#fff8f4]/95 backdrop-blur-md sticky top-0 z-50 transition-colors border-b border-primary/10 w-full';
        headerEl.innerHTML = `
            <div class="max-w-[1400px] mx-auto px-4 sm:px-6 lg:px-12 h-20 flex items-center justify-between">
                <!-- Brand -->
                <a class="flex items-center gap-3 decoration-transparent" href="/index.html">
                    <img alt="InChef Emblem" class="w-10 h-10 object-contain rounded-full shadow-xs" src="/images/inchef_logo_v2.png" onerror="this.onerror=null; this.src='/images/logo.png';">
                    <span class="text-2xl font-extrabold text-[#004538] tracking-tight">InChef</span>
                </a>

                <!-- Nav Links (Desktop) -->
                <nav class="hidden md:flex items-center gap-7 lg:gap-9">
                    ${navLinks.map(link => `
                        <a href="${link.href}" class="nav-link font-medium text-sm lg:text-base ${active === link.id ? 'text-primary font-bold border-b-2 border-primary pb-1' : 'text-on-surface-variant hover:text-primary transition-colors'}">
                            ${link.label}
                        </a>
                    `).join('')}
                </nav>

                <!-- Auth / Profile Actions -->
                <div class="flex items-center gap-3">
                    ${authSection}
                    <!-- Mobile Hamburger -->
                    <button id="header-mobile-toggle" class="md:hidden p-2 rounded-xl text-primary hover:bg-primary/10 transition-colors" aria-label="Toggle Navigation">
                        <span class="material-symbols-outlined text-[26px]">menu</span>
                    </button>
                </div>
            </div>

            <!-- Mobile Drawer -->
            <div id="header-mobile-drawer" class="hidden md:hidden bg-surface-container-low border-b border-outline-variant px-6 py-4 space-y-2">
                ${navLinks.map(link => `
                    <a href="${link.href}" class="block py-2 text-sm font-semibold ${active === link.id ? 'text-primary font-bold' : 'text-on-surface-variant hover:text-primary'}">
                        ${link.label}
                    </a>
                `).join('')}
                ${user ? `
                    <div class="pt-2 border-t border-outline-variant/60 flex items-center justify-between">
                        <a href="/profile.html" class="text-sm font-bold text-primary flex items-center gap-2">
                            <span class="material-symbols-outlined text-[18px]">account_circle</span> My Profile
                        </a>
                    </div>
                ` : `
                    <div class="pt-2 border-t border-outline-variant/60 flex items-center gap-3">
                        <a href="/auth.html?mode=login" class="text-xs font-bold text-primary py-2">Login</a>
                        <a href="/auth.html?mode=signup" class="text-xs font-bold bg-primary text-white px-4 py-2 rounded-full">Sign Up</a>
                    </div>
                `}
            </div>
        `;

        // Wire mobile drawer toggle
        const toggleBtn = document.getElementById('header-mobile-toggle');
        const drawer = document.getElementById('header-mobile-drawer');
        if (toggleBtn && drawer) {
            toggleBtn.addEventListener('click', () => {
                drawer.classList.toggle('hidden');
            });
        }
    }

    function renderFooter() {
        let footerEl = document.getElementById('global-footer');
        if (!footerEl) {
            footerEl = document.createElement('footer');
            footerEl.id = 'global-footer';
            document.body.appendChild(footerEl);
        }

        footerEl.className = 'bg-[#1f1b17] text-[#efe7e0] py-12 mt-auto border-t border-[#34302b]';
        footerEl.innerHTML = `
            <div class="max-w-[1400px] mx-auto px-4 sm:px-6 lg:px-12 flex flex-col md:flex-row items-center justify-between gap-6 text-sm">
                <div class="flex items-center gap-3">
                    <img src="/images/inchef_logo_v2.png" alt="InChef" class="w-8 h-8 rounded-full bg-white p-0.5 object-contain" onerror="this.onerror=null; this.src='/images/logo.png';" />
                    <span class="font-extrabold text-lg text-white tracking-tight">InChef</span>
                    <span class="text-xs text-[#bfc9c4]">| Authentic Indian Recipe Recommendations</span>
                </div>
                <div class="flex flex-wrap justify-center gap-6 text-xs text-[#cac7c0]">
                    <a href="/index.html" class="hover:text-white transition-colors">Home</a>
                    <a href="/discover.html" class="hover:text-white transition-colors">Discover Recipes</a>
                    <a href="/recommendations.html" class="hover:text-white transition-colors">Recommendations</a>
                    <a href="/my_kitchen.html" class="hover:text-white transition-colors">My Kitchen</a>
                    <a href="/favorites.html" class="hover:text-white transition-colors">Favorites</a>
                    <a href="/profile.html" class="hover:text-white transition-colors">Profile & Preferences</a>
                </div>
                <div class="text-xs text-[#707975]">
                    &copy; 2025 InChef. Cook what you have, love what you eat.
                </div>
            </div>
        `;
    }

    document.addEventListener('DOMContentLoaded', () => {
        renderHeader();
        renderFooter();
    });
})();
