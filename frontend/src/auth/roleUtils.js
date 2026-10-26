export const normalizeRole = (role) => {
  switch (role) {
    case 'USER':
      return 'END_USER';
    case 'TECH':
      return 'TECHNICIAN';
    case 'END_USER':
    case 'TECHNICIAN':
    case 'ADMIN':
      return role;
    default:
      return role || 'UNKNOWN';
  }
};

export const normalizeUser = (user) => {
  if (!user || typeof user !== 'object') {
    return null;
  }

  return {
    id: user.id,
    name: user.name,
    email: user.email,
    role: normalizeRole(user.role),
    rawRole: user.role,
  };
};