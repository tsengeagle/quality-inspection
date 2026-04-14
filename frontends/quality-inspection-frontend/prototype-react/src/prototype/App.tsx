import { Navigate, Route, Routes } from 'react-router-dom';
import { HomeScreen } from './screens/HomeScreen';
import { CaseDetailScreen } from './screens/CaseDetailScreen';
import { RevisionHistoryScreen } from './screens/RevisionHistoryScreen';
import { RereviewScreen } from './screens/RereviewScreen';
import { WorkQueueScreen } from './screens/WorkQueueScreen';
import { AssignmentScreen } from './screens/AssignmentScreen';
import { InitialReviewScreen } from './screens/InitialReviewScreen';

export function App() {
  return (
    <Routes>
      <Route path="/" element={<HomeScreen />} />
      <Route path="/review-harness" element={<HomeScreen />} />
      <Route path="/cases" element={<WorkQueueScreen />} />
      <Route path="/cases/:caseId/detail" element={<CaseDetailScreen />} />
      <Route path="/cases/:caseId/assign" element={<AssignmentScreen />} />
      <Route path="/cases/:caseId/initial-review" element={<InitialReviewScreen />} />
      <Route path="/cases/:caseId/revisions" element={<RevisionHistoryScreen />} />
      <Route path="/cases/:caseId/rereview" element={<RereviewScreen />} />
      <Route path="*" element={<Navigate replace to="/" />} />
    </Routes>
  );
}
