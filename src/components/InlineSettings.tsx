import React, { useState } from 'react';
import { StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { COLORS } from '../theme/colors';

interface InlineSettingsProps {
  apiKey: string;
  isConfigured: boolean;
  onSave: (key: string) => void;
  onClose: () => void;
}

export const InlineSettings: React.FC<InlineSettingsProps> = ({
  apiKey,
  isConfigured,
  onSave,
  onClose,
}) => {
  const [inputKey, setInputKey] = useState(apiKey);

  return (
    <View style={styles.card}>
      <View style={styles.header}>
        <Text style={styles.title}>OpenRouteService API Key</Text>
        <TouchableOpacity onPress={onClose} hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}>
          <Text style={styles.closeBtn}>✕</Text>
        </TouchableOpacity>
      </View>

      <TextInput
        style={styles.input}
        placeholder="Enter 5b3ce3597851110001..."
        placeholderTextColor={COLORS.mutedText}
        value={inputKey}
        onChangeText={setInputKey}
        secureTextEntry
        autoCapitalize="none"
        autoCorrect={false}
      />

      <View style={styles.footer}>
        <Text style={styles.hint}>
          {isConfigured ? 'Key saved locally' : 'Required for geocoding & directions'}
        </Text>
        <TouchableOpacity
          style={styles.saveBtn}
          onPress={() => onSave(inputKey)}
          activeOpacity={0.8}
        >
          <Text style={styles.saveBtnText}>Save</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  card: {
    backgroundColor: COLORS.surface,
    borderRadius: 12,
    padding: 16,
    marginVertical: 12,
    borderWidth: 1,
    borderColor: COLORS.surfaceVariant,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 10,
  },
  title: {
    color: COLORS.text,
    fontSize: 14,
    fontWeight: '600',
  },
  closeBtn: {
    color: COLORS.mutedText,
    fontSize: 16,
    padding: 4,
  },
  input: {
    backgroundColor: COLORS.background,
    borderWidth: 1,
    borderColor: COLORS.surfaceVariant,
    borderRadius: 8,
    color: COLORS.text,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 14,
    marginBottom: 12,
  },
  footer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  hint: {
    color: COLORS.amberAccent,
    fontSize: 12,
  },
  saveBtn: {
    backgroundColor: COLORS.amberAccent,
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 8,
  },
  saveBtnText: {
    color: COLORS.background,
    fontWeight: '700',
    fontSize: 13,
  },
});
