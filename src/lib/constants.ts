export const COLORS = {
  gold: "#ffcc84",
  goldLight: "#ffcd84",
  text: "#464b44",
  textAlt: "#464b45",
  dark: "#4d5046",
  pink: "#ffdcdc",
  cream: "#ffecec",
  overlay: "rgba(111, 90, 61, 0.2)",
} as const;

export const NAV_LINKS = [
  { label: "Start", href: "#start" },
  { label: "Features", href: "#features" },
  { label: "pricing", href: "#pricing" },
  { label: "about", href: "#about" },
] as const;

export const FOOTER_COLUMNS = [
  {
    title: "Product",
    links: ["Features", "Solutions", "AI Agents", "Pricing"],
  },
  {
    title: "Company",
    links: ["About", "Careers", "Partners", "Contact"],
  },
  {
    title: "Resources",
    links: ["Blog", "Documentation", "FAQs", "Help Center"],
  },
  {
    title: "Legal",
    links: ["Privacy Policy", "Terms & Conditions", "Security", "Cookie Policy"],
  },
] as const;

export const PROCESS_STEPS = [
  { label: "IDEA", highlight: true, className: "left-[5px] top-0 w-[123px]" },
  { label: "Website", highlight: false, className: "left-[-57px] top-[58px] w-[325px]" },
  { label: "Hosting", highlight: false, className: "left-[250px] top-[109px] w-[177px]" },
  { label: "Payments", highlight: true, className: "left-[377px] top-[185px] w-[484px]" },
  { label: "Marketing", highlight: false, className: "left-[162px] top-[236px] w-[210px]" },
  { label: "Emails", highlight: false, className: "left-[5px] top-[306px] w-[177px]" },
  { label: "Analytics", highlight: false, className: "left-[156px] top-[371px] w-[542px]" },
  { label: "Growth", highlight: false, className: "left-[344px] top-[456px] w-[177px]" },
  { label: "Scale", highlight: false, className: "left-[-57px] top-[554px] w-[691px]" },
] as const;

export const HIGHLIGHT_WORDS = ["marketing", "operations"] as const;
