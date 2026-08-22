"use client";

import { useState } from "react";
import Image from "next/image";
import { NAV_LINKS } from "@/lib/constants";
import { Logo } from "./Logo";
import { cn } from "@/lib/utils";

export function Navbar() {
  const [open, setOpen] = useState(false);

  return (
    <header className="absolute inset-x-0 top-0 z-30 px-3 pt-3 sm:px-4">
      <div className="relative flex items-start justify-between">
        <div className="relative z-10 flex items-center gap-2">
          <div className="rounded-[13.7px] bg-[#ffcc84] px-3 py-2">
            <Logo />
          </div>
          <p className="hidden text-[9px] text-white sm:block">A Catifaal product</p>
        </div>

        <nav className="absolute left-1/2 top-1 hidden -translate-x-1/2 md:block">
          <div className="flex h-[26px] items-center rounded-full bg-[#ffcd84] px-6">
            <ul className="flex items-center gap-9 text-[9.4px] font-bold uppercase tracking-wide text-[#464b45]">
              {NAV_LINKS.map((link) => (
                <li key={link.label}>
                  <a href={link.href} className="transition-opacity hover:opacity-70">
                    {link.label}
                  </a>
                </li>
              ))}
            </ul>
          </div>
        </nav>

        <div className="flex items-center gap-2">
          <button
            type="button"
            aria-label="Toggle menu"
            className="flex h-[26px] w-10 items-center justify-center rounded-full bg-[#ffcd84] text-[10px] font-bold text-[#464b45] md:hidden"
            onClick={() => setOpen((prev) => !prev)}
          >
            {open ? "✕" : "☰"}
          </button>
          <a
            href="#login"
            className="hidden h-[26px] min-w-[68px] items-center justify-center rounded-full bg-[#ffcd84] text-[9.4px] font-bold text-[#464b45] md:inline-flex"
          >
            Login
          </a>
        </div>
      </div>

      <div
        className={cn(
          "mt-2 overflow-hidden rounded-2xl bg-[#ffcd84] transition-all duration-300 md:hidden",
          open ? "max-h-64 opacity-100" : "max-h-0 opacity-0"
        )}
      >
        <ul className="flex flex-col gap-3 px-4 py-4 text-[11px] font-bold text-[#464b45]">
          {NAV_LINKS.map((link) => (
            <li key={link.label}>
              <a href={link.href} onClick={() => setOpen(false)}>
                {link.label}
              </a>
            </li>
          ))}
          <li>
            <a href="#login" onClick={() => setOpen(false)}>
              Login
            </a>
          </li>
        </ul>
      </div>

      <div className="pointer-events-none absolute right-[165px] top-0 hidden lg:block">
        <Image
          src="/images/shape-accent.png"
          alt=""
          width={67}
          height={67}
          aria-hidden
        />
      </div>
    </header>
  );
}
