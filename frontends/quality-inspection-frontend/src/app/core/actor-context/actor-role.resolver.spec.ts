import {
  canonicalizeRole,
  canonicalizeRoles,
  normalizeActorContext,
  resolvePrimaryRole
} from './actor-role.resolver';

describe('actor-role resolver', () => {
  it('maps legacy aliases to canonical roles', () => {
    expect(canonicalizeRole('MANAGER')).toBe('HOSPITAL_MANAGER');
    expect(canonicalizeRole('ADMIN')).toBe('SYSTEM_ADMIN');
    expect(canonicalizeRole('SYSADMIN')).toBe('SYSTEM_ADMIN');
  });

  it('uses currRole as the first primary-role priority after canonicalization', () => {
    expect(resolvePrimaryRole('manager', ['REVIEWER', 'SYSTEM_ADMIN'])).toBe('HOSPITAL_MANAGER');
  });

  it('falls back to the fixed priority order when currRole is absent', () => {
    expect(resolvePrimaryRole(null, ['REVIEWER', 'AUDITOR'])).toBe('AUDITOR');
  });

  it('normalizes the raw shell state into the shared actor context shape', () => {
    expect(normalizeActorContext({
      userInfo: {
        userId: 'U-001',
        userName: 'Alice',
        hospitalId: 'H-001'
      },
      userRoles: ['MANAGER', 'REVIEWER'],
      currentRole: null
    })).toEqual({
      userId: 'U-001',
      userName: 'Alice',
      hospitalId: 'H-001',
      roles: ['HOSPITAL_MANAGER', 'REVIEWER'],
      currentRole: 'HOSPITAL_MANAGER'
    });
  });

  it('deduplicates canonical roles', () => {
    expect(canonicalizeRoles(['MANAGER', 'HOSPITAL_MANAGER', 'REVIEWER'])).toEqual([
      'HOSPITAL_MANAGER',
      'REVIEWER'
    ]);
  });
});
