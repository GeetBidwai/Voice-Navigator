import { NavCommandType } from '../../types/navigation';

/**
 * Deterministic rule-based command parser in TypeScript.
 * No AI, LLM, or cloud inference is used.
 */
export function parseVoiceCommand(rawInput: string): NavCommandType {
  const cleaned = rawInput
    .toLowerCase()
    .replace(/[?!.,']/g, '')
    .trim();

  if (!cleaned) {
    return { type: 'UNKNOWN', rawText: rawInput };
  }

  // 1. Stop Navigation
  const stopKeywords = [
    'stop navigation', 'stop navigating', 'end navigation',
    'cancel navigation', 'stop route', 'cancel route',
    'end route', 'stop', 'exit navigation', 'quit navigation',
  ];
  if (stopKeywords.some((kw) => cleaned === kw || cleaned.startsWith(kw))) {
    return { type: 'STOP_NAVIGATION' };
  }

  // 2. Repeat
  const repeatKeywords = [
    'repeat', 'repeat instruction', 'say again', 'repeat last',
    'repeat that', 'what was that', 'pardon', 'again',
  ];
  if (repeatKeywords.some((kw) => cleaned === kw || cleaned.startsWith(kw))) {
    return { type: 'REPEAT' };
  }

  // 3. How far / ETA
  const distanceKeywords = [
    'how far', 'how far is it', 'distance', 'distance remaining',
    'remaining distance', 'how much distance', 'how long',
    'time remaining', 'eta', 'when will we arrive',
  ];
  if (distanceKeywords.some((kw) => cleaned === kw || cleaned.startsWith(kw))) {
    return { type: 'HOW_FAR' };
  }

  // 4. Next turn
  const nextTurnKeywords = [
    'next turn', 'whats next turn', 'what is next turn',
    'what\'s next turn', 'what is the next turn', 'which turn',
    'where to turn', 'directions', 'next direction', 'next step',
  ];
  if (nextTurnKeywords.some((kw) => cleaned === kw || cleaned.startsWith(kw))) {
    return { type: 'NEXT_TURN' };
  }

  // 5. Reroute
  const rerouteKeywords = [
    'reroute', 'recalculate', 'recalculate route',
    'find another way', 'different route', 'alternative route',
  ];
  if (rerouteKeywords.some((kw) => cleaned === kw || cleaned.startsWith(kw))) {
    return { type: 'REROUTE' };
  }

  // 6. Help
  if (cleaned === 'help' || cleaned === 'commands' || cleaned === 'what can i say') {
    return { type: 'HELP' };
  }

  // 7. Nearest POI
  const poiMatch = cleaned.match(
    /(?:nearest|find|locate|search for|closest)\s+(?:a\s+)?(petrol pump|fuel station|gas station|petrol bunk|puncture shop|mechanic|garage|hospital|pharmacy|chemist)/i
  ) || cleaned.match(/^(petrol pump|fuel station|gas station|petrol bunk|puncture shop|mechanic|hospital)$/i);

  if (poiMatch) {
    let poi = poiMatch[1].toLowerCase();
    if (['fuel station', 'gas station', 'petrol bunk'].includes(poi)) {
      poi = 'petrol pump';
    }
    return { type: 'FIND_NEAREST', poiType: poi };
  }

  // 8. Destination Navigation
  const destMatch = cleaned.match(
    /^(?:take me to|navigate to|go to|directions to|route to|head to|drive to|ride to)\s+(.+)$/i
  ) || cleaned.match(/^(?:take me)\s+(.+)$/i);

  if (destMatch && destMatch[1]?.trim()) {
    return { type: 'NAVIGATE_TO', destination: toTitleCase(destMatch[1].trim()) };
  }

  // Direct place name fallback (1-4 words)
  const words = cleaned.split(' ');
  if (words.length >= 1 && words.length <= 4 && !cleaned.includes('stop')) {
    return { type: 'NAVIGATE_TO', destination: toTitleCase(cleaned) };
  }

  return { type: 'UNKNOWN', rawText: rawInput };
}

function toTitleCase(str: string): string {
  return str.replace(/\w\S*/g, (txt) => txt.charAt(0).toUpperCase() + txt.substring(1).toLowerCase());
}
