import Image from "next/image";
import { FOOTER_COLUMNS } from "@/lib/constants";
import { ScriptText } from "./ScriptText";

const socialIcons = [
  { src: "/images/social-x.png", label: "X" },
  { src: "/images/social-linkedin.png", label: "LinkedIn" },
  { src: "/images/social-instagram.png", label: "Instagram" },
  { src: "/images/social-facebook.png", label: "Facebook" },
  { src: "/images/social-youtube.png", label: "YouTube" },
  { src: "/images/social-github.png", label: "GitHub" },
  { src: "/images/social-discord.png", label: "Discord" },
];

function FooterColumn({
  title,
  links,
}: {
  title: string;
  links: readonly string[];
}) {
  return (
    <div>
      <h3 className="mb-3 text-[14px] font-normal text-[#ffecec] sm:text-[18px]">
        {title}
      </h3>
      <ul className="space-y-2">
        {links.map((link) => (
          <li key={link}>
            <a
              href={`#${link.toLowerCase().replace(/\s+/g, "-")}`}
              className="text-[11px] text-[#ffecec] transition-opacity hover:opacity-70 sm:text-[13px]"
            >
              {link}
            </a>
          </li>
        ))}
      </ul>
    </div>
  );
}

export function Footer() {
  return (
    <footer className="relative mt-8 overflow-hidden">
      <div className="absolute -top-[98px] left-1/2 z-10 h-[98px] w-[790px] max-w-[120%] -translate-x-1/2">
        <Image
          src="/images/footer-curve.png"
          alt=""
          fill
          className="object-cover"
          aria-hidden
        />
      </div>

      <div className="relative min-h-[420px] overflow-hidden">
        <Image
          src="/images/footer-bg.png"
          alt=""
          fill
          className="scale-y-[-1] rotate-180 object-cover"
          aria-hidden
        />
        <div className="absolute inset-0 bg-[rgba(255,205,132,0.2)]" />
        <div className="absolute inset-0 bg-[rgba(0,0,0,0.4)]" />

        <div className="relative z-10 px-5 pb-8 pt-16 sm:px-8">
          <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
            <p className="text-[64px] leading-none text-[#464b44] sm:text-[122px]">
              <span className="text-[#ffecec]">M</span>ozaara
            </p>
            <div className="flex items-end gap-1 text-white">
              <ScriptText className="text-[34px] sm:text-[43px]">M</ScriptText>
              <p className="text-[18px] sm:text-[22px]">ake it exist</p>
            </div>
          </div>

          <div className="mb-10 grid grid-cols-2 gap-6 sm:grid-cols-4">
            {FOOTER_COLUMNS.map((column) => (
              <FooterColumn
                key={column.title}
                title={column.title}
                links={column.links}
              />
            ))}
          </div>

          <div className="flex flex-col gap-6 border-t border-white/10 pt-6 sm:flex-row sm:items-end sm:justify-between">
            <div className="text-[9px] leading-relaxed text-[#ffecec]">
              <p>© 2026 Catifaal Systems</p>
              <p>Made with ❤️ in India</p>
            </div>

            <div className="flex flex-col items-start gap-4 sm:items-end">
              <p className="text-[9px] text-[#ffecec]">
                Follow Us
                <br />
                <br />
                hello@mozaara.ai
              </p>
              <div className="flex items-center gap-4">
                {socialIcons.map((icon) => (
                  <a
                    key={icon.label}
                    href="#"
                    aria-label={icon.label}
                    className="transition-opacity hover:opacity-70"
                  >
                    <Image
                      src={icon.src}
                      alt=""
                      width={17}
                      height={17}
                    />
                  </a>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </footer>
  );
}
