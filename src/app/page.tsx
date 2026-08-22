import {
  Hero,
  ValueProposition,
  BusinessChallenge,
  ProcessFlow,
  IdeaScaleSection,
  BuiltInPieces,
  Footer,
} from "@/components/landing";

export default function Home() {
  return (
    <div className="min-h-screen bg-[#ffcc84]">
      <main className="mx-auto min-h-screen w-full max-w-[633px] bg-[#ffcc84]">
        <Hero />
        <ValueProposition />
        <BusinessChallenge />
        <ProcessFlow />
        <IdeaScaleSection />
        <BuiltInPieces />
        <Footer />
      </main>
    </div>
  );
}
