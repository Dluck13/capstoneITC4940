export default function TicketCoopLogo({ size = 72, title = 'TicketCoop logo', className = '' }) {
  const labelled = typeof title === 'string' && title.trim().length > 0;

  return (
    <svg
      className={`ticketcoop-logo ${className}`.trim()}
      width={size}
      height={size}
      viewBox="0 0 128 128"
      role="img"
      aria-hidden={labelled ? undefined : true}
      aria-label={labelled ? title : undefined}
    >
      {labelled ? <title>{title}</title> : null}

      <ellipse className="logo-halo" cx="64" cy="66" rx="30" ry="34" />

      <g className="logo-orbit" transform="rotate(-12 64 64)">
        <path className="logo-orbit-back" d="M14 66 C22 41 106 41 114 66" />
        <path className="logo-orbit-aux" d="M20 66 C30 48 98 48 108 66" />
      </g>

      <path
        className="logo-outline"
        transform="translate(0 128) scale(1 -1)"
        d="M64 14 C50 14 40 25 38 42 C35 61 41 83 53 96 C58 102 70 102 75 96 C87 83 93 61 90 42 C88 25 78 14 64 14 Z"
      />

      <path className="logo-trace" d="M64 92 V66" />
      <path className="logo-trace" d="M64 74 L52 66 V54" />
      <path className="logo-trace" d="M64 80 L78 71 V58" />
      <path className="logo-trace" d="M64 86 L48 76 V64" />
      <path className="logo-trace-thin" d="M64 69 L64 57" />

      <circle className="logo-node" cx="52" cy="52" r="2.5" />
      <circle className="logo-node" cx="78" cy="56" r="2.5" />
      <circle className="logo-node" cx="48" cy="62" r="2.5" />
      <circle className="logo-node logo-node-core" cx="64" cy="92" r="2.9" />

      <g className="logo-orbit" transform="rotate(-12 64 64)">
        <path className="logo-orbit-front" d="M14 66 C22 91 106 91 114 66" />
      </g>
    </svg>
  );
}
