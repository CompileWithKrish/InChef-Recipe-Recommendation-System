# InChef Design System



## Brand & Style
The design system is built for a premium, productivity-focused culinary experience. The brand personality is grounded, intelligent, and serene—moving away from the frantic energy of typical recipe apps toward a calm, organized kitchen companion. 

The aesthetic follows a **refined modern minimalism** approach, utilizing generous whitespace and a "warm-gallery" feel. It prioritizes clarity and utility, ensuring that the interface never competes with the photography of the food itself. Visual interest is generated through precise typography and a sophisticated organic palette rather than decorative flourishes.

## Layout & Spacing
The system follows a strict 8px linear scale. Layouts should prioritize a sense of "breathing room" to maintain the minimalist aesthetic.

- **Grid Model:** Use a 12-column fluid grid for desktop and a 4-column grid for mobile.
- **Section Spacing:** Use 80px (10 units) between major vertical sections on desktop to maintain a premium, airy feel.
- **Component Padding:** Internal card padding should be a consistent 24px (3 units) to ensure content feels framed rather than cramped.

## Elevation & Depth
Depth is created through a combination of **tonal layering** and **soft ambient shadows**. 

- **Level 0 (Background):** Warm Ivory surface.
- **Level 1 (Cards):** Pure White surface with a 1px border in Warm Grey (#E5E1DA) and a soft, highly diffused shadow (0px 4px 20px rgba(42, 38, 34, 0.05)).
- **Level 2 (Interactive/Hover):** Increase shadow spread and slightly darken the border to create a "lifted" effect.
- **Depth Principle:** No heavy shadows or harsh blacks. All depth cues must feel natural, as if soft light is hitting a physical paper card.

## Components
- **Buttons:** Primary buttons are solid Deep Emerald with white text. Secondary/CTA buttons (like "Start Cooking") use Warm Amber. Ghost buttons use Deep Emerald text with no background.
- **Cards:** White background, 1px warm-grey border, 18px corner radius. Image content should fill the top half of the card with no internal margin, while text content resides in a padded 24px area below.
- **Input Fields:** 12px radius, 1px border. On focus, the border transitions to Deep Emerald with a 2px stroke.
- **Chips/Badges:** Small, 12px radius, using a subtle tint of the primary color (Deep Emerald at 10% opacity) for categorization, and Warm Amber for high-priority alerts like "Missing Ingredient."
- **Lists:** Ingredient lists should feature a custom checkbox using a Deep Emerald fill when active. High horizontal padding (16px) between the icon and text to aid legibility at a distance.
- **Progress Indicators:** Simple, thin 4px bars using Deep Emerald to track recipe steps, avoiding complex circular loaders.