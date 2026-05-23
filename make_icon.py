#!/usr/bin/env python3
"""
Génère une icône simple pour Flemmix TV
"""
import os

# Icône SVG simple : fond noir, lettre F rouge style Netflix
svg_icon = '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 192 192">
  <rect width="192" height="192" rx="20" fill="#141414"/>
  <text x="96" y="150" font-family="Arial Black, sans-serif" font-size="140" 
        font-weight="900" fill="#E50914" text-anchor="middle">F</text>
</svg>'''

with open('/tmp/icon.svg', 'w') as f:
    f.write(svg_icon)

print("SVG créé")
